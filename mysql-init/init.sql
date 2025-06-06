CREATE DATABASE IF NOT EXISTS `delivery_service_schema`;
CREATE DATABASE IF NOT EXISTS `live_auction_schema`;
CREATE DATABASE IF NOT EXISTS `notification_schema`;
CREATE DATABASE IF NOT EXISTS `order_service_schema`;
CREATE DATABASE IF NOT EXISTS `payment_service_schema`;
CREATE DATABASE IF NOT EXISTS `product_schema`;
CREATE DATABASE IF NOT EXISTS `timed_auction_schema`;
CREATE DATABASE IF NOT EXISTS `user_schema`;

CREATE USER 'deliveries_svc_user'@'%' IDENTIFIED BY 'quan12345656915691';
GRANT ALL PRIVILEGES ON `delivery_service_schema`.* TO 'deliveries_svc_user'@'%';

CREATE USER 'live_auctions_svc_user'@'%' IDENTIFIED BY 'quan12345656915691';
GRANT ALL PRIVILEGES ON `live_auction_schema`.* TO 'live_auctions_svc_user'@'%';

CREATE USER 'notifications_svc_user'@'%' IDENTIFIED BY 'quan12345656915691';
GRANT ALL PRIVILEGES ON `notification_schema`.* TO 'notifications_svc_user'@'%';

CREATE USER 'orders_svc_user'@'%' IDENTIFIED BY 'quan12345656915691';
GRANT ALL PRIVILEGES ON `order_service_schema`.* TO 'orders_svc_user'@'%';

CREATE USER 'payments_svc_user'@'%' IDENTIFIED BY 'quan12345656915691';
GRANT ALL PRIVILEGES ON `payment_service_schema`.* TO 'payments_svc_user'@'%';

CREATE USER 'products_svc_user'@'%' IDENTIFIED BY 'quan12345656915691';
GRANT ALL PRIVILEGES ON `product_schema`.* TO 'products_svc_user'@'%';

CREATE USER 'timed_auctions_svc_user'@'%' IDENTIFIED BY 'quan12345656915691';
GRANT ALL PRIVILEGES ON `timed_auction_schema`.* TO 'timed_auctions_svc_user'@'%';

CREATE USER 'users_svc_user'@'%' IDENTIFIED BY 'quan12345656915691';
GRANT ALL PRIVILEGES ON `user_schema`.* TO 'users_svc_user'@'%';

FLUSH PRIVILEGES;

USE `product_schema`;

CREATE TABLE IF NOT EXISTS `categories` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    parent_id BIGINT DEFAULT NULL,
    CONSTRAINT fk_parent_category FOREIGN KEY (parent_id) REFERENCES categories(id)
);


INSERT INTO `categories` VALUES (1,'Books',NULL),(2,'Furniture',NULL),(3,'Fashion & Costume',NULL),(4,'Electronics (Household - Limited)',NULL),(5,'Collectibles & Hobbies',NULL),(101,'Fiction Books',1),(102,'Non-Fiction Books',1),(103,'Comics & Graphic Novels',1),(104,'Children\'s & Young Adult Books',1),(105,'Textbooks & Educational Books',1),(106,'Foreign Language Books',1),(201,'Seating Furniture',2),(202,'Tables Furniture',2),(203,'Storage Furniture',2),(204,'Beds & Bedroom Furniture',2),(205,'Outdoor Furniture',2),(206,'Other Furniture',2),(301,'Women\'s Clothing',3),(302,'Men\'s Clothing',3),(303,'Shoes',3),(304,'Bags & Wallets',3),(305,'Fashion Accessories',3),(306,'Costumes & Cosplay',3),(401,'Small Kitchen Appliances',4),(402,'Small Refrigeration',4),(403,'Small Home Appliances',4),(404,'Small Audio Equipment',4),(501,'Trading Cards',5),(502,'Stamps (Collectibles)',5),(503,'Coins & Banknotes',5),(504,'Toys & Figures (Collectibles)',5),(505,'Media Collectibles (Physical)',5),(506,'Memorabilia',5),(507,'Posters & Art Prints',5),(599,'Random Collectibles / Other',5),(10101,'Contemporary Fiction',101),(10102,'Classic Literature',101),(10103,'Thriller, Crime & Mystery',101),(10104,'Sci-Fi & Fantasy',101),(10105,'Romance Fiction',101),(10106,'Historical Fiction',101),(10107,'Other Fiction',101),(10201,'Biography & Memoir',102),(10202,'History (Non-Fiction)',102),(10203,'Science & Nature Books',102),(10204,'Business & Finance Books',102),(10205,'Self-Help & Psychology Books',102),(10206,'Cooking & Food Books',102),(10207,'Travel Books',102),(10208,'Other Non-Fiction',102),(10301,'Manga',103),(10302,'Manhwa / Manhua',103),(10303,'Comics (Western/VN)',103),(10304,'Graphic Novels',103),(10601,'English Language Books',106),(10602,'Japanese Language Books',106),(10603,'Korean Language Books',106),(10604,'French Language Books',106),(10605,'Other Language Books',106),(20101,'Sofas & Couches',201),(20102,'Chairs (Dining, Office, etc.)',201),(20103,'Stools & Benches',201),(20201,'Dining Tables & Sets',202),(20202,'Coffee & Side Tables',202),(20203,'Desks & Office Tables',202),(20301,'Bookshelves & Display Units',203),(20302,'Cabinets & Sideboards',203),(20303,'Wardrobes & Closets',203),(20304,'Chests of Drawers',203),(20305,'TV Stands & Media Units',203),(20401,'Bed Frames',204),(20402,'Nightstands',204),(20403,'Dressers & Vanities',204),(30101,'Women\'s Dresses',301),(30102,'Women\'s Skirts',301),(30103,'Women\'s Tops (Shirts, Blouses, T-Shirts)',301),(30104,'Women\'s Pants & Jeans',301),(30105,'Women\'s Shorts',301),(30106,'Women\'s Outerwear (Jackets, Coats)',301),(30107,'Women\'s Traditional Wear (Ao Dai, etc.)',301),(30201,'Men\'s Shirts (Dress, Casual, T-Shirts)',302),(30202,'Men\'s Pants & Jeans',302),(30203,'Men\'s Shorts',302),(30204,'Men\'s Outerwear (Jackets, Coats)',302),(30301,'Women\'s Shoes',303),(30302,'Men\'s Shoes',303),(30303,'Unisex Shoes',303),(30501,'Watches',305),(30502,'Jewelry (Necklaces, Bracelets, etc.)',305),(30503,'Belts',305),(30504,'Hats & Caps',305),(30505,'Scarves & Shawls',305),(30601,'Full Costumes',306),(30602,'Cosplay Wigs',306),(30603,'Cosplay Props & Accessories',306),(30604,'Themed Apparel',306),(40101,'Microwaves',401),(40102,'Ovens & Toasters',401),(40103,'Rice Cookers',401),(40104,'Blenders, Mixers & Juicers',401),(40105,'Kettles & Coffee Makers',401),(40106,'Air Fryers',401),(50101,'Pokemon Cards',501),(50102,'Yugioh Cards',501),(50103,'Magic: The Gathering Cards',501),(50104,'Other TCGs',501),(50401,'Action Figures & Statues',504),(50402,'Model Kits (Gundam, Cars, etc.)',504),(50403,'Dolls & Plushies',504),(50404,'Board Games & Puzzles',504),(50405,'Vintage & Retro Toys',504),(50501,'Vinyl Records',505),(50502,'CDs & DVDs',505),(50503,'Cassette Tapes',505),(50504,'Video Games (Physical Copies)',505);
