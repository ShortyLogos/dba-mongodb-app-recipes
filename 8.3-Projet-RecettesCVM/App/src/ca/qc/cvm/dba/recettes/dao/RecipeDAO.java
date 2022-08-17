package ca.qc.cvm.dba.recettes.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ca.qc.cvm.dba.recettes.entity.Ingredient;
import ca.qc.cvm.dba.recettes.entity.Recipe;
import ca.qc.cvm.dba.recettes.dao.MongoConnection;
import ca.qc.cvm.dba.recettes.dao.BerkeleyConnection;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import org.bson.Document;

public class RecipeDAO {

	/**
	 * M�thode permettant de sauvegarder une recette
	 * 
	 * Notes importantes:
	 * - Si le champ "id" n'est pas null, alors c'est une mise � jour, autrement c'est une insertion
	 * - Le nom de la recette doit �tre unique
	 * - Regarder comment est fait la classe Recette et Ingredient pour avoir une id�e des donn�es � sauvegarder
	 * - � l'insertion, il doit y avoir un id num�rique unique associ� � la recette. 
	 *   D�pendemment de la base de donn�es, vous devrez trouver une strat�gie pour faire un id num�rique.
	 * 
	 * @param //recette
	 * @return true si succ�s, false sinon
	 */
	public static boolean save(final Recipe recipe) {
		boolean success = false;
		try {
			MongoDatabase conMongo = MongoConnection.getConnection();
			MongoCollection<Document> collRecipes = conMongo.getCollection("recipes");
			final Database conBerkeley = BerkeleyConnection.getConnection();
			
			//MongoCollection<Document> collIngredients = conMongo.getCollection("ingredients"); <-----------------------------------------------------

			Document doc = new Document ();
			doc.append("name", recipe.getName().toUpperCase());
			doc.append("portion", recipe.getPortion());
			doc.append("prepTime", recipe.getPrepTime());
			doc.append("cookTime", recipe.getCookTime());
			doc.append("steps", recipe.getSteps());

			List<Document> ingredientsList = new ArrayList<Document>();
			for (Ingredient i: recipe.getIngredients()) {
				String name = i.getName().toUpperCase();

				Document ingDoc = new Document();
				ingDoc.append("name", name);
				ingDoc.append("quantity", i.getQuantity());

				ingredientsList.add(ingDoc);
				//collIngredients.insertOne(new Document("name", name)); <-----------------------------------------------------
			}
			doc.append("ingredients", ingredientsList);

			collRecipes.insertOne(doc);
			
			// On effectue une requête suite à l'insertion pour obtenir le ID généré aléatoirement
			// par MongoDB et l'utiliser comme clé dans BerkeleyDB
			FindIterable<Document> iterator = collRecipes.find(new Document("name", recipe.getName()));
			try {
				iterator.forEach(new Block<Document>() {
					@Override
					public void apply(final Document document) {
						String cleRecipe = document.getObjectId("_id").toString();
						recipe.setId(Long.parseLong(cleRecipe));
						byte[] image = recipe.getImageData();
						try {
							DatabaseEntry theKey = new DatabaseEntry(cleRecipe.getBytes("UTF-8"));
							DatabaseEntry theData = new DatabaseEntry(image);
							conBerkeley.put(null, theKey, theData);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				success = true;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	/**
	 * M�thode permettant de retourner la liste des recettes de la base de donn�es.
	 * 
	 * Notes importantes:
	 * - N'oubliez pas de limiter les r�sultats en fonction du param�tre limit
	 * - La liste doit �tre tri�es en ordre croissant, selon le nom des recettes
	 * - Le champ filtre doit permettre de filtrer selon le pr�fixe du nom (insensible � la casse)
	 * - N'oubliez pas de mettre l'ID dans la recette
	 * - Il pourrait ne pas y avoir de filtre (champ filtre vide)
	 * 	 * 
	 * @param filter champ filtre, peut �tre vide ou null
	 * @param limit permet de restreindre les r�sultats
	 * @return la liste des recettes, selon le filtre si n�cessaire 
	 */
	public static List<Recipe> getRecipeList(String filter, int limit) {
		final List<Recipe> recipeList = new ArrayList<Recipe>();

		try {
			MongoDatabase conMongo = MongoConnection.getConnection();
			MongoCollection<Document> collection = conMongo.getCollection("recipes");
			
			Document query = new Document();
//			if (filter != "") {
//				filter = "/^"+filter.toUpperCase()+"/";
//				query = new Document("name", new Document("$regex", filter));
//			}
	
			Document oderBy = new Document("name", 1);
			FindIterable<Document> iterator = collection.find(query).sort(oderBy).limit(limit);
			
			iterator.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					Recipe recipe = new Recipe();
					Long id = (long)document.getObjectId("_id").getTimestamp();
					recipe.setId(id);
					recipe.setName(document.getString("name"));
					recipe.setPortion(document.getInteger("portion"));
					recipe.setPrepTime(document.getInteger("prepTime"));
					recipe.setCookTime(document.getInteger("cookTime"));
					recipe.setSteps((List<String>)document.get("steps"));

					List<Document> ingredientsList = (List<Document>)document.get("ingredients");
					List<Ingredient> ingredients = new ArrayList<Ingredient>();
					for (Document doc : ingredientsList) {
						String ingName = doc.getString("name");
						String ingQte = doc.getString("quantity");
						ingredients.add(new Ingredient(ingName, ingQte));
					}
					recipe.setIngredients(ingredients);
					
					// Récupérer l'image depuis 
					recipe.setImageData(null);
					
					recipeList.add(recipe);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		return recipeList;
	}

	/**
	 * Suppression des donn�es li�es � une recette
	 * 
	 * @param recipe
	 * @return true si succ�s, false sinon
	 */
	public static boolean delete(Recipe recipe) {
		boolean success = false;
		String filter = String.valueOf(recipe.getId());

		try {
			MongoDatabase conMongo = MongoConnection.getConnection();
			MongoCollection<Document> collRecipes = conMongo.getCollection("recipes");
			collRecipes.deleteOne(new Document("_id", filter));
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return success;
	}
	
	/**
	 * Suppression totale de toutes les donn�es du syst�me!
	 * 
	 * @return true si succ�s, false sinon
	 */
	public static boolean deleteAll() {
		boolean success = false;

		try {
			MongoDatabase conMongo = MongoConnection.getConnection();
			MongoCollection<Document> collRecipes = conMongo.getCollection("recipes");
			collRecipes.deleteMany(new Document());
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return success;
	}
	
	/**
	 * Permet de retourner le nombre d'ingr�dients en moyenne dans une recette
	 * 
	 * @return le nombre moyen d'ingr�dients
	 */
	public static double getAverageNumberOfIngredients() {
		double num = 0;
				
		return num;
	}
	
	/**
	 * Permet d'obtenir le temps de la recette la plus longue � faire.
	 * 
	 * La recette la plus longue est calcul�e selon son temps de cuisson plus son temps de pr�paration
	 * 
	 * @return le temps maximal
	 */
	public static long getMaxRecipeTime() {
		long num = 0;
				
		return num;
	}
	
	/**
	 * Permet d'obtenir le nombre de photos dans la base de donn�es BerkeleyDB
	 * 
	 * @return nombre de photos dans BerkeleyDB
	 */
	public static long getPhotoCount() {
		long num = 0;
		
		return num;
	}

	/**
	 * Permet d'obtenir le nombre de recettes dans votre base de donn�es
	 * 
	 * @return nombre de recettes
	 */
	public static long getRecipeCount() {
		long num = 0;
		
		return num;
	}
	
	/**
	 * Permet d'obtenir la derni�re recette ajout�e dans le syst�me
	 * 
	 * @return la derni�re recette
	 */
	public static Recipe getLastAddedRecipe() {
		Recipe recipe = null;
		
		
		return recipe;
	}
	
	/**
	 * Cette fonctionnalit� permet de g�n�rer une recette en se basant sur celles existantes 
	 * dans le syst�me. Voici l'algorithme g�n�rale � utiliser :
	 * 
	 * 1- Allez chercher tous les ingr�dients dans votre base de donn�es
	 * 2- Construisez une liste al�atoire d'ingr�dients selon les ingr�dients obtenus � l'�tape pr�c�dente
	 * 3- Cr�ez une liste al�atoire de quelques �tapes bas�e sur une liste pr�d�finie(ex : "M�langez tous les ingr�dients", "cuire au four 20 minutes", etc)
	 * 4- Faites un temps de cuisson, de pr�paration et de nombre de portions al�atoires
	 * 5- Copiez une image d'une autre recette
	 * 6- Construisez un nom en utilisant cette logique :
	 *    - un pr�fixe al�atoire parmi une liste pr�d�finie (ex: ["Giblotte �", "M�lang� de", "Crastillon de"]
	 *    - un suffixe bas� sur un des ingr�dients de la recette (ex: "farine").
	 *    - R�sultat fictif : Crastillon � farine
	 * 
	 * Laissez l'ID de le recette vide, et ne l'ajoutez pas dans la base de donn�es.
	 * 
	 * @return une recette g�n�r�e
	 */
	public static Recipe generateRandomRecipe() {
		Recipe r = new Recipe();
		
		return r;
	}
	
	/**
	 * Permet d'obtenir une liste de noms de recette similaires � une autre recette
	 * 
	 * - En se basant sur les ingr�dients d'une recette existante (obtenue par recipeId)
	 * - Trouver les recettes qui ont les ingr�dients les plus similaires. 
	 * - Les ordonner DESC (selon le nombre d'ingr�dients similaires), limiter les r�sultats (limit), 
	 * - puis retourner le nom de ces recettes 
	 * 
	 * @param recipeId id de la recette
	 * @param limit nombre � retourner
	 * @return
	 */
	public static List<String> getSimilarRecipes(long recipeId, int limit) {
		List<String> recipeList = new ArrayList<String>();

		return recipeList;
	}
}
