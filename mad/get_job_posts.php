<?php
header("Content-Type: application/json");
require 'config.php';

// Optionally filter by role later, but for now return ALL job posts
try {
    $sql = "SELECT jp.id, jp.user_id, jp.title, jp.description, jp.media_url, jp.created_at,
                   u.name AS recruiter_name, u.email AS recruiter_email
            FROM job_posts jp
            JOIN users u ON jp.user_id = u.id
            ORDER BY jp.created_at DESC";
    $stmt = $conn->prepare($sql);
    $stmt->execute();
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode([
        "success" => true,
        "data" => $rows
    ]);

} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => $e->getMessage()
    ]);
}
