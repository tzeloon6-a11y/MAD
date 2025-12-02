<?php
header("Content-Type: application/json");
require 'config.php';

// Allow only POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(["success" => false, "message" => "Invalid request method"]);
    exit;
}

$user_id    = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$title      = isset($_POST['title']) ? trim($_POST['title']) : '';
$desc       = isset($_POST['description']) ? trim($_POST['description']) : '';
$media_url  = isset($_POST['media_url']) ? trim($_POST['media_url']) : null;

if ($user_id <= 0 || $title === '' || $desc === '') {
    echo json_encode(["success" => false, "message" => "Missing required fields"]);
    exit;
}

try {
    $sql = "INSERT INTO job_posts (user_id, title, description, media_url)
            VALUES (:user_id, :title, :description, :media_url)";
    $stmt = $conn->prepare($sql);
    $stmt->bindParam(':user_id', $user_id);
    $stmt->bindParam(':title', $title);
    $stmt->bindParam(':description', $desc);
    $stmt->bindParam(':media_url', $media_url);
    $stmt->execute();

    echo json_encode(["success" => true, "message" => "Job post saved"]);

} catch (PDOException $e) {
    echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
