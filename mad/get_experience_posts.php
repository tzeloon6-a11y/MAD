<?php
header("Content-Type: application/json");

require 'config.php';

// Optional: ?user_id=1 to get posts for a specific student
$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;

try {
    if ($user_id > 0) {
        $stmt = $conn->prepare("
            SELECT id, user_id, title, description, media_url, created_at
            FROM experience_posts
            WHERE user_id = :user_id
            ORDER BY created_at DESC
        ");
        $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
        $stmt->execute();
    } else {
        // If no user_id given, return all experience posts (for testing)
        $stmt = $conn->prepare("
            SELECT id, user_id, title, description, media_url, created_at
            FROM experience_posts
            ORDER BY created_at DESC
        ");
        $stmt->execute();
    }

    $posts = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode([
        "success" => true,
        "data" => $posts
    ]);

} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => $e->getMessage()
    ]);
}
?>
