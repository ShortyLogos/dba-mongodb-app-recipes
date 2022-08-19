# RECETTES-CVM - PROJET FINAL DU COURS DBA
Projet réalisé à partir d'une solution polyglote MongoDB + BerkeleyDB

## ÉQUIPE
Déric Marchand & Karl Marchand

## COLLECTIONS
Une seule collection 'recipes'. Les ingrédients sont gérés
à l'intérieur des recettes et l'incrémentation des ID est
effectué au moyen du tandem MongoDB - Java.

## INDEX UTILISÉS
1. Index unique sur les noms des recettes (croissant)    
Note : L'ordre croissant et décroissant n'a pas vraiment d'importance sur les index appliqués sur un seul champ avec MongoDB
```
db.recipes.createIndex( { "name": 1 }, { unique: true } );
```

2. Index sur les ingrédients des recettes (croissant)
```
db.recipes.createIndex( { "ingredients": 1 } );
```

## NOTE SUR LA CONTRAINTE DU 'ID' DE LA CLASSE RECETTE
Parce que le projet impose un ID de type 'Long' dans la classe Recipe,
il est difficile d'utiliser les clés '_id' générés automatiquement par MongoDB.
Nous avons tenté de le faire en utilisant simplement la partie 'Timestamp' du hash
afin de s'en servir comme clé dans la base de données Berkeley 
et en tant que id pour les objets Recipe.

Cependant, cela implique que l'on ne peut pas récupérer une recette (instance de la classe
Recipe dans Java) à partir de son Id, car il manque des informations pour reconstruire
le hex du '_id' dans son entièreté. Nous avons tenté de le transformer en String et puis en Long,
avec notamment les méthodes .toHexString() et Long.paseLong(), mais le résultat total
dépasse la capacité d'un Long.

Nous avons donc été contraint d'ignorer le '_id' généré aléatoirement par MongoDB et de l'écraser
avec un système maison : si aucune recette n'existe, le '_id' est un Long de 0. Sinon, le '_id'
équivaut à celui de la dernière recette ajoutée auquel on ajoute 1.

## REQUÊTES PURES DE MONGODB
Notre approche a été de construire la majorité du temps nos requêtes à l'intérieur du shell de MongoDB.
Une fois qu'elles étaient fonctionnelles, nous entreprenions le travail de « traduction » imposé par le
connecteur Java. À des fins de documentation et de consultation future, voici une requête pure plutôt lourde
qu'il a fallu « traduire » pour obtenir les recettes qui ont des ingrédients similaires.

```
db.recipes.aggregate([ {$match: {"ingredients.name": {$in: ["CHAMPIGNON","FROMAGE","SAUCE HOISIN"]}}}, {$project: {_id: "$name", similarIngredientCount: {$size: {$filter: {input: "$ingredients.name", cond: {$in: ["$$this", ["FROMAGE", "CHAMPIGNON","SAUCE HOISIN"] ]}} }}}}, {$sort: { "similarIngredientCount": -1 }}, {$limit: 5} ]);
```

## SOURCES
La documentation de MongoDB pour l'ensemble du projet :
https://www.mongodb.com/docs/

Synthaxe des expressions régulières en Java :
https://stackoverflow.com/questions/32714333/java-mongodb-regex-query

Fonction utilitaire clamp en Java:
https://stackoverflow.com/questions/16656651/does-java-have-a-clamp-function

