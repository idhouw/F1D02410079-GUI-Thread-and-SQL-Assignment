# F1D02410079-GUI-Thread-and-SQL-Assignment

<img width="479" height="380" alt="image" src="https://github.com/user-attachments/assets/a9c1f698-6f0f-46e0-97b1-01151662c8cb" />

# Anime List Management

A desktop application built with **Java Swing** for managing an anime collection. It provides CRUD (Create, Read, Update, Delete) operations, search functionality by title or genre, and anime rating management.

### Thread Usage
Threads are used for database operations through the `runDb()` method. Operations such as loading, adding, updating, and deleting data run in a *background thread* to keep the user interface responsive and prevent freezing. `SwingUtilities.invokeLater()` is used to safely update the UI.

### SQL Usage
MySQL is used as the database to store anime information, with database connectivity handled through JDBC. The `schema.sql` file creates the `db_anime` database and `anime` table, as well as inserts some initial anime records.

### Technologies
- Java Swing
- Java Threads
- MySQL
- JDBC
