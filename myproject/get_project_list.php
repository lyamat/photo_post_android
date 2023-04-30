<?php

$pdo = new PDO('mysql:host=localhost;dbname=myproject', 'root', '');

$stmt = $pdo->query('SELECT project_id, project_name FROM project');
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

$json = json_encode($rows);

file_put_contents('projects.json', $json);

header('Content-Type: application/json');
echo $json;
?>
