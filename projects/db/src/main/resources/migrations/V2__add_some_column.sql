
ALTER TABLE Person RENAME TO Person_temporary;
CREATE TABLE Person (id INTEGER CONSTRAINT PK_PERSON PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, age INTEGER, address_id INTEGER, bar TEXT NOT NULL, blub INTEGER NOT NULL, foo TEXT);
INSERT INTO Person (id,name,age,address_id,bar,blub) SELECT id,name,age,address_id,bar,blub FROM Person_temporary;
DROP TABLE Person_temporary;
REINDEX Person;

ALTER TABLE Address RENAME TO Address_temporary;
CREATE TABLE Address (id INTEGER CONSTRAINT PK_ADDRESS PRIMARY KEY AUTOINCREMENT NOT NULL, street TEXT, city TEXT NOT NULL, street_number TEXT);
INSERT INTO Address (id,street,city) SELECT id,street,city FROM Address_temporary;
DROP TABLE Address_temporary;
REINDEX Address;

ALTER TABLE Address RENAME TO Address_temporary;
CREATE TABLE Address (id INTEGER CONSTRAINT PK_ADDRESS PRIMARY KEY AUTOINCREMENT NOT NULL, street TEXT, city TEXT NOT NULL, street_number TEXT);
INSERT INTO Address (id,street,city,street_number) SELECT id,street,city,street_number FROM Address_temporary;
DROP TABLE Address_temporary;
REINDEX Address;

