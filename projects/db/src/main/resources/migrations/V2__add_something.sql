
ALTER TABLE Person RENAME TO Person_temporary;
CREATE TABLE Person (id INTEGER CONSTRAINT PK_PERSON PRIMARY KEY AUTOINCREMENT NOT NULL, foo TEXT, name TEXT NOT NULL, age INTEGER, hello TEXT);
INSERT INTO Person (id,foo,name,age) SELECT id,foo,name,age FROM Person_temporary;
DROP TABLE Person_temporary;
REINDEX Person;

ALTER TABLE Address RENAME TO Address_temporary;
CREATE TABLE Address (id INTEGER CONSTRAINT PK_ADDRESS PRIMARY KEY AUTOINCREMENT NOT NULL, street TEXT NOT NULL, street_number TEXT);
INSERT INTO Address (id,street,street_number) SELECT id,street,street_number FROM Address_temporary;
DROP TABLE Address_temporary;
REINDEX Address;

