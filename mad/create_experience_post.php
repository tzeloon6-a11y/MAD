<?php
header("Content-Type: application/json");

// Only allow POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(["success" => false, "message" => "Invalid request method"]);
    exit;
}

require 'config.php';

// Get data from POST
$user_id     = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$title       = isset($_POST['title']) ? trim($_POST['title']) : '';
$description = isset($_POST['description']) ? trim($_POST['description']) : '';
$media_url   = isset($_POST['media_url']) ? trim($_POST['media_url']) : null; // optional

// Basic validation
if ($user_id <= 0 || $title === '' || $description === '') {
    echo json_encode(["success" => false, "message" => "Missing fields"]);
    exit;
}

try {
    $sql = "INSERT INTO experience_posts (user_id, title, description, media_url)
            VALUES (:user_id, :title, :description, :media_url)";
    $stmt = $conn->prepare($sql);
    $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
    $stmt->bindParam(':title', $title, PDO::PARAM_STR);
    $stmt->bindParam(':description', $description, PDO::PARAM_STR);
    $stmt->bindParam(':media_url', $media_url, PDO::PARAM_STR);

    if ($stmt->execute()) {
        echo json_encode(["success" => true, "message" => "Experience post created"]);
    } else {
        echo json_encode(["success" => false, "message" => "Insert failed"]);
    }

} catch (PDOException $e) {
    echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
?>
