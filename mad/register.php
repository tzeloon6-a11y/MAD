<?php
require 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo "Invalid request method";
    exit;
}

$name = isset($_POST['name']) ? trim($_POST['name']) : '';
$email = isset($_POST['email']) ? trim($_POST['email']) : '';
$password = isset($_POST['password']) ? trim($_POST['password']) : '';
$role = isset($_POST['role']) ? trim($_POST['role']) : 'student';

if ($name === '' || $email === '' || $password === '') {
    echo "Missing fields";
    exit;
}

// Check if email already exists
$stmt = $conn->prepare("SELECT id FROM users WHERE email = :email");
$stmt->bindParam(':email', $email);
$stmt->execute();
if ($stmt->fetch()) {
    echo "Email already exists";
    exit;
}

// For now, store plain password in password_hash (like we did for login)
try {
    $stmt = $conn->prepare("INSERT INTO users (name, email, password_hash, role) 
                            VALUES (:name, :email, :password_hash, :role)");
    $stmt->bindParam(':name', $name);
    $stmt->bindParam(':email', $email);
    $stmt->bindParam(':password_hash', $password);
    $stmt->bindParam(':role', $role);
    $stmt->execute();

    echo "success";
} catch (PDOException $e) {
    echo "DB error: " . $e->getMessage();
}
