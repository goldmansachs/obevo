/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
db.mytable.dropIndex( { name: "ind1" } );
db.mytable.createIndex( { name: "ind1" }, { category2: 1 } );
db.mytable.dropIndex( { name: "date_category_fr" } );
db.mytable.createIndex(
   { orderDate: 1, category: 1 },
   { name: "date_category_fr", collation: { locale: "fr", strength: 2 } }
)
db.mytable.createIndex(
   { abcdef: 2, ghijkl: 2 },
   { name: "date_category_fr2", collation: { locale: "fr", strength: 2 } }
)
db.mytable.dropIndex( { name: "ind2" } );
db.mytable.createIndex( { name: "ind2" }, { category: 1 } );
