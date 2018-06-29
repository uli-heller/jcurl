Mein persönliches README
========================

... brauche ich in erster Linie, weil ich
mich mit Maven nicht so recht auskenne.

Eclipse
-------

### Lombok

In Eclipse muß "lombok" aktiviert sein!

* Herunterladen: [https://projectlombok.org/download](https://projectlombok.org/download') - Version 1.18.0
* Aktivieren: `java -jar lombok.jar`

### Projekt erzeugen

```
mvn eclipse:eclipse
```

### Importieren

... innerhalb von Eclipse.

Bauen
-----

```
mvn package
```

Ausführen
---------

```
java -jar jcurl-all.jar
```
