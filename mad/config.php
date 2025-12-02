<?php
// config.php - database connection for local XAMPP

$host = "127.0.0.1";
$db_name = "mad";   // the DB name you created in phpMyAdmin
$db_user = "root";        // default XAMPP MySQL user
$db_pass = "";            // default XAMPP MySQL password is empty

try {
    $conn = new PDO("mysql:host=$host;dbname=$db_name;charset=utf8", $db_user, $db_pass);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    // If you want, you can echo something for debugging:
    // echo "Connected successfully";
} catch (PDOException $e) {
    // Return JSON error (helpful for debugging in browser)
    die(json_encode([
        "success" => false,
        "message" => "Connection failed: " . $e->getMessage()
    ]));
}
?>
