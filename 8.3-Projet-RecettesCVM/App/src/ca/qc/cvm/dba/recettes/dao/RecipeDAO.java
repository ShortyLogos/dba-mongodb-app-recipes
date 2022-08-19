package ca.qc.cvm.dba.recettes.dao;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import ca.qc.cvm.dba.recettes.entity.Ingredient;
import ca.qc.cvm.dba.recettes.entity.Recipe;
import ca.qc.cvm.dba.recettes.dao.MongoConnection;
import ca.qc.cvm.dba.recettes.dao.BerkeleyConnection;
import com.mongodb.Block;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;

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
		String recipeName = recipe.getName().toUpperCase();
		
		try {
			MongoDatabase conMongo = MongoConnection.getConnection();
			MongoCollection<Document> collRecipes = conMongo.getCollection("recipes");
			final Database conBerkeley = BerkeleyConnection.getConnection();

			Document doc = new Document ();
			doc.append("name", recipeName);
			doc.append("portion", recipe.getPortion());
			doc.append("prepTime", recipe.getPrepTime());
			doc.append("cookTime", recipe.getCookTime());
			doc.append("steps", recipe.getSteps());

			List<Document> ingredientsList = new ArrayList<Document>();
			for (Ingredient ing: recipe.getIngredients()) {
				Document ingDoc = new Document();
				
				ingDoc.append("name", ing.getName().toUpperCase());
				ingDoc.append("quantity", ing.getQuantity());

				ingredientsList.add(ingDoc);
			}
			doc.append("ingredients", ingredientsList);

			if (recipe.getId() == null) {
				long id = 0;
				try {
					Recipe lastAddedRecipe = getLastAddedRecipe();
					id = lastAddedRecipe.getId() + 1;
				}
				catch (NullPointerException ne) {
					System.out.println("Première recette! Espérons que ce soit le début d'une belle aventure.");
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				finally {
					recipe.setId(id);
					doc.append("_id", id);
				}
				
				collRecipes.insertOne(doc);
			}
			else {
				Bson filter = Filters.eq("name", recipeName);				
				collRecipes.replaceOne(filter, doc);
			}
			
			byte[] image = recipe.getImageData();
			
			try {
				DatabaseEntry theKey = new DatabaseEntry(String.valueOf(recipe.getId()).getBytes("UTF-8"));
				DatabaseEntry theData = new DatabaseEntry(image);
				conBerkeley.put(null, theKey, theData);
				
				success = true;
			} catch (Exception e) {
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
		boolean displayKeys = true;

		if (displayKeys) {
			testBerkeley();
		}

		final List<Recipe> recipeList = new ArrayList<Recipe>();
		try {
			MongoDatabase conMongo = MongoConnection.getConnection();
			MongoCollection<Document> collection = conMongo.getCollection("recipes");
			
			Document query = new Document();
			
			if (filter.length() > 0) {
				query.append("name", new Document("$regex", "^(?i)"+Pattern.quote(filter)));
			}
	
			Document orderBy = new Document("name", 1);
			FindIterable<Document> iterator = collection.find(query).sort(orderBy).limit(limit);
			
			iterator.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					Recipe recipe = convertDocToRecipe(document);
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
        
        try {
            MongoDatabase conMongo = MongoConnection.getConnection();
            MongoCollection<Document> collRecipes = conMongo.getCollection("recipes");
            Database conBerkeley = BerkeleyConnection.getConnection();  
        
            String key = String.valueOf(recipe.getId());
            DatabaseEntry theKey = new DatabaseEntry(key.getBytes("UTF-8"));
            
            collRecipes.deleteOne(new Document("name", recipe.getName()));
            conBerkeley.delete(null, theKey);
            
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
		Cursor myCursor = null;

		try {
			MongoDatabase conMongo = MongoConnection.getConnection();
			MongoCollection<Document> collRecipes = conMongo.getCollection("recipes");
			collRecipes.deleteMany(new Document());
			
			final Database conBerkeley = BerkeleyConnection.getConnection();
			List<String> myList = new ArrayList<>(); 
			try {
				myCursor = conBerkeley.openCursor(null, null);
				DatabaseEntry foundKey = new DatabaseEntry();
			    DatabaseEntry foundData = new DatabaseEntry();
			    while (myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			    	myList.add(new String(foundKey.getData(), "UTF-8"));
			    }
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (myCursor != null) {
			            myCursor.close();
			        }
			    } catch(DatabaseException dbe) {
			        System.err.println("Erreur de fermeture du curseur: " + dbe.toString());
			    }
			}
		    for (String key: myList) {
		    	conBerkeley.delete(null, new DatabaseEntry(key.getBytes("UTF-8")));
		    }
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
		
		MongoDatabase conMongo = MongoConnection.getConnection();
		MongoCollection<Document> collRecipes = conMongo.getCollection("recipes");

		Document group = new Document(
			"$group", new Document("_id", null).append(
				"average", new Document( "$avg", new Document("$size","$ingredients") )
			)
		);

		List<Document> list = new ArrayList<Document>();
		list.add(group);

		final List<Document> results = new ArrayList<Document>();

		AggregateIterable<Document> iterable = collRecipes.aggregate(list );
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				results.add(document);
			}
		});

		num = results.get(0).getDouble("average");
		
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
		
		try {
			MongoDatabase conMongo = MongoConnection.getConnection();
			MongoCollection<Document> collRecipes = conMongo.getCollection("recipes");
			
			BsonArray fieldList = new BsonArray();
			fieldList.add(new BsonString("$prepTime"));
			fieldList.add(new BsonString("$cookTime"));
			AggregateIterable<Document> iterable = collRecipes.aggregate(Arrays.asList(
						new Document("$project", new Document("total", new Document("$add", fieldList))),
						new Document("$sort", new Document("total", -1)),
						new Document("$limit", 1)
					));
			
			final List<Document> results = new ArrayList<Document>();
			iterable.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					results.add(document);
				}
			});
			num = (long)(results.get(0).getInteger("total"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		return num;
	}
	
	/**
	 * Permet d'obtenir le nombre de photos dans la base de donn�es BerkeleyDB
	 * 
	 * @return nombre de photos dans BerkeleyDB
	 */
	public static long getPhotoCount() {
		long num = 0;
		Cursor myCursor = null;

		try {
			final Database conBerkeley = BerkeleyConnection.getConnection();
		    myCursor = conBerkeley.openCursor(null, null);
		 
		    DatabaseEntry foundKey = new DatabaseEntry();
		    DatabaseEntry foundData = new DatabaseEntry();
		 
		    while (myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
		        num++;
		    }
		} 
		catch (DatabaseException de) {
		    System.err.println("Erreur de lecture de la base de données: " + de);
		} 
		finally {
		    try {
		        if (myCursor != null) {
		            myCursor.close();
		        }
		    } 
		 catch(DatabaseException dbe) {
		        System.err.println("Erreur de fermeture du curseur: " + dbe.toString());
		    }
		}
		
		return num;
	}

	/**
	 * Permet d'obtenir le nombre de recettes dans votre base de donn�es
	 * 
	 * @return nombre de recettes
	 */
	public static long getRecipeCount() {
		long num = 0;
		
		try {
			MongoDatabase conMongo = MongoConnection.getConnection();
			MongoCollection<Document> collRecipes = conMongo.getCollection("recipes");
			num = collRecipes.count();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return num;
	}
	
	/**
	 * Permet d'obtenir la derni�re recette ajout�e dans le syst�me
	 * 
	 * @return la derni�re recette
	 */
	public static Recipe getLastAddedRecipe() {
		Recipe recipe = null;
		
		try {
			MongoDatabase conMongo = MongoConnection.getConnection();
			MongoCollection<Document> collRecipes = conMongo.getCollection("recipes");
			Document doc = collRecipes.find().sort(new Document("_id", -1)).first();
			if (doc != null) {
				recipe = convertDocToRecipe(doc);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
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
		final List<String> recipeList = new ArrayList<String>();
		
		try {
			MongoDatabase conMongo = MongoConnection.getConnection();
			MongoCollection<Document> collRecipes = conMongo.getCollection("recipes");

			Document recipeDoc = collRecipes.find(new Document("_id", recipeId)).first();
			
			ArrayList<Document> ingredients = (ArrayList)recipeDoc.get("ingredients");
			BsonArray ingArray = new BsonArray();
			
			for (Document i : ingredients) {
				ingArray.add(new BsonString(i.getString("name")));
			}
			
			BsonArray recipeMatchCount = new BsonArray();
			recipeMatchCount.add(new BsonString("$$this"));
			recipeMatchCount.add(ingArray);
			
			Document match = new Document("$match", new Document("ingredients.name", new Document("$in", ingArray)));
			
			Document filter = new Document();
			filter.append("input", "$ingredients.name");
			filter.append("cond", new Document("$in", recipeMatchCount));
						
			Document projectPipeline = new Document();
			projectPipeline.append("_id", "$name");
			projectPipeline.append("similarIngredientsCount", new Document("$size", new Document("$filter", filter)));
			
			Document project = new Document("$project", projectPipeline);
			Document sort = new Document("$sort", new Document("similarIngredientsCount", -1));
			Document limitResults = new Document("$limit", limit);
			
			AggregateIterable<Document> iterable = collRecipes.aggregate(Arrays.asList(
						match,
						project,
						sort,
						limitResults
					));
			
			iterable.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					recipeList.add(document.get("_id").toString());
				}
			});			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	
		// db.recipes.aggregate([ 
		// {$match: {"ingredients.name": {$in: ["CHAMPIGNON","FROMAGE","SAUCE HOISIN"]}}}, 
		// {$project: {_id: "$name", similarIngredientCount: {$size: {$filter: {input: "$ingredients.name", cond: {$in: ["$$this", ["FROMAGE", "CHAMPIGNON","SAUCE HOISIN"] ]}} }}}}, 
		// {$sort: { "similarIngredientCount": -1 }}, 
		// {$limit: 1} ]); 

		return recipeList;
	}
	
	
	
	public static void testBerkeley() {
		Database connection = BerkeleyConnection.getConnection();
		Cursor myCursor = null;
		
		System.out.println("Database dump: ");
		try {
		    myCursor = connection.openCursor(null, null);
		 
		    DatabaseEntry foundKey = new DatabaseEntry();
		    DatabaseEntry foundData = new DatabaseEntry();
		 
		    while (myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
		        String keyString = new String(foundKey.getData(), "UTF-8");
		        System.out.println(foundKey);
		    }
		} 
		catch (DatabaseException de) {
		    System.err.println("Erreur de lecture de la base de données: " + de);
		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
		finally {
		    try {
		        if (myCursor != null) {
		            myCursor.close();
		        }
		    } 
		 catch(DatabaseException dbe) {
		        System.err.println("Erreur de fermeture du curseur: " + dbe.toString());
		    }
		}
		
		System.out.println("Test terminé.");
	}
	
	public static Recipe convertDocToRecipe(Document document) {
		Recipe recipe = new Recipe();
			
		recipe.setId((document.getLong("_id")));
		recipe.setName(document.getString("name"));
		recipe.setPortion(document.getInteger("portion"));
		recipe.setPrepTime(document.getInteger("prepTime"));
		recipe.setCookTime(document.getInteger("cookTime"));
		recipe.setSteps((List<String>)document.get("steps"));
		
		// Insertion des ingrédients
		List<Ingredient> ingredients = new ArrayList<Ingredient>();
		List<Document> ingredientsList = (List<Document>)document.get("ingredients");
		for (Document doc : ingredientsList) {
			String ingName = doc.getString("name");
			String ingQte = doc.getString("quantity");
			ingredients.add(new Ingredient(ingName, ingQte));
		}
		recipe.setIngredients(ingredients);
		
		// Récupérer l'image depuis BerkeleyDB
		try {
			Database conBerkeley = BerkeleyConnection.getConnection();
			
			DatabaseEntry theKey = new DatabaseEntry(String.valueOf(recipe.getId()).getBytes("UTF-8"));
		    DatabaseEntry theData = new DatabaseEntry();
		    
		    OperationStatus status = conBerkeley.get(null, theKey, theData, LockMode.DEFAULT);
		    if (status == OperationStatus.SUCCESS) { 
		        byte[] imgData = theData.getData();
		        recipe.setImageData(imgData);
		    } 

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return recipe;
	}
	
}
