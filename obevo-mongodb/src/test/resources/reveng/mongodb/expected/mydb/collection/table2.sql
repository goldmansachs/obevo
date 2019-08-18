//// CHANGE INDEX name=tab2ind1
db.table2.createIndex(
	{ "orderDate" : 1.0, "category" : 1.0 }
	, { "name" : "tab2ind1", "collation" : { "locale" : "fr", "caseLevel" : false, "caseFirst" : "off", "strength" : 2, "numericOrdering" : false, "alternate" : "non-ignorable", "maxVariable" : "punct", "normalization" : false, "backwards" : false, "version" : "57.1" } }
);
