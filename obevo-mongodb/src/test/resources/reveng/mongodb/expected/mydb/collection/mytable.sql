db.mytable.createIndex(
	{ "orderDate" : 1.0, "category" : 1.0 }
	, { "name" : "date_category_fr", "collation" : { "locale" : "fr", "caseLevel" : false, "caseFirst" : "off", "strength" : 2, "numericOrdering" : false, "alternate" : "non-ignorable", "maxVariable" : "punct", "normalization" : false, "backwards" : false, "version" : "57.1" } }
);