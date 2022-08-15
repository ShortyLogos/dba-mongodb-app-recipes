package ca.qc.cvm.dba.recettes.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.qc.cvm.dba.recettes.entity.Recipe;

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
	 * @param recette
	 * @return true si succ�s, false sinon
	 */
	public static boolean save(Recipe recipe) {
		boolean success = false;
				
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
		List<Recipe> recipeList = new ArrayList<Recipe>();
		
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
	
		return success;
	}
	
	/**
	 * Suppression totale de toutes les donn�es du syst�me!
	 * 
	 * @return true si succ�s, false sinon
	 */
	public static boolean deleteAll() {
		boolean success = false;
					
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
