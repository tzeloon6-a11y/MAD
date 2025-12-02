<?php
header("Content-Type: application/json");

// Only allow POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(["success" => false, "message" => "Invalid request method"]);
    exit;
}

require 'config.php';

$email = isset($_POST['email']) ? trim($_POST['email']) : '';
$password = isset($_POST['password']) ? trim($_POST['password']) : '';

if ($email === '' || $password === '') {
    echo json_encode(["success" => false, "message" => "Missing email or password"]);
    exit;
}

try {
    // Find user by email
    $stmt = $conn->prepare("SELECT id, name, email, password_hash, role FROM users WHERE email = :email");
    $stmt->bindParam(':email', $email);
    $stmt->execute();
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user) {
        echo json_encode(["success" => false, "message" => "Invalid email or password"]);
        exit;
    }

    // For now, compare plain text (password_hash column stores plain password)
    if ($password !== $user['password_hash']) {
        echo json_encode(["success" => false, "message" => "Invalid email or password"]);
        exit;
    }

    // Success
    echo json_encode([
        "success" => true,
        "message" => "Login success",
        "user" => [
            "id" => (int)$user['id'],
            "name" => $user['name'],
            "email" => $user['email'],
            "role" => $user['role']
        ]
    ]);

} catch (PDOException $e) {
    echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
?>
