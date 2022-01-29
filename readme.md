# Tenacity

**Tenacity is currently alpha software. Use with caution.**

Tenacity is a [Paper](https://papermc.io/) plugin for sharing the same inventory and player data across multiple
different minecraft servers (in a [Velocity](https://velocitypowered.com/) network, for example). It works by storing
player data into a database, separate from the actual Minecraft servers. Every time a player joins the server, their
data is fetched from the database and assigned to the player. Whenever a player leaves the server, their data is
written back into the database.

## Setup
To run Tenacity you will need a [MySQL](https://www.mysql.com/) compatible database server 
(like [MariaDB](https://mariadb.org/)). You will need a user that can create tables and insert
data into a database of your choice. Here's how to create one:

```mysql
CREATE USER 'tenacity'@'localhost' IDENTIFIED BY 'ASecurePassword';
CREATE DATABASE tenacity;
GRANT ALL PRIVILEGES ON tenacity.* TO 'tenacity'@'localhost';
```

The name of the user and database are up to your discretion.

After starting your server once with Tenacity installed, a configuration file will be created in the 
`plugins/tenacity/` folder. You will have to modify it to match the database you just set up. For the
example above, a configuration like this would be valid:

```yaml
database:
  username: 'tenacity'
  password: 'ASecurePassword'
  host: 'localhost'
  port: 3306
  name: 'tenacity'
saving:
  health: true
  experience: true
  food: true
  effects: true
  recipeBook: true
  inventory: true
```

You can also customize what player data should be shared across servers by setting the values under `saving` to 
`true` or `false`. The actual configuration file contains more information about the different values.

## Building
Tenacity is a Gradle project. To build it, you will need an up-to-date build of JDK 17 installed
on your machine. To get started, download the source code (either by downloading the ZIP file or
`git clone`-ing it). Then open the folder with the source code in a terminal or command prompt
and run `./gradlew shadowJar`. You will find the plugin's JAR file in `./build/libs`.
