# RECETTES-CVM - PROJET FINAL DU COURS DBA
Projet réalisé à partir d'une solution polyglote MongoDB + BerkeleyDB

## ÉQUIPE
Déric Marchand
Karl Marchand

## COLLECTIONS
Une seule collection 'recipes'

## INDEX UTILISÉS
1. Index unique sur les noms des recettes (croissant)    
// En MongoDB, l'ordre croissant et décroissant n'a pas vraiment d'importance sur les index appliqués sur un seul champ
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
Nous le faisons actuellement en retirant la partie 'Timestamp' du hash
afin de s'en servir comme clé dans la base de données Berkeley 
et en tant que id pour les objets Recipe.

Cependant, cela implique que lorsqu'on désire mettre à jour à une recette
existante, on peut tout modifier à l'exception du nom. En effet, nous sommes
forcés d'utiliser le nom de la recette pour effectuer une requête au sein
de la base de données MongoDB, car nous ne pouvons pas reconstituer la clé '_id'
de manière complète à partir de la seule partie Timestamp de l'ObjectID.

Si nous disposions de la liberté de soit changer le type de la variable d'instance
'Id' de la classe Recipe en tant que String ou encore d'ajouter une variable du style 'mongoId' au sein
de la classe pour y entreposer le '_id' généré par MongoDB, nous aurions pu facilement
échapper à toutes ces complications.<

## SOURCES
Synthaxe des expression régulières en Java :
https://stackoverflow.com/questions/32714333/java-mongodb-regex-query

