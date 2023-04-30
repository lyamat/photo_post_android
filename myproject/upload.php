<?php

header('Content-Type: text/html; charset=utf-8');
mb_internal_encoding("UTF-8");

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  
  $imageBase64 = $_POST['imageBase64'];
  $projectName = $_POST['project_name'];
  $comment = $_POST['comment'];

  // Create a JSON file with the current date and time as a filename
  date_default_timezone_set('Europe/Moscow');
  $dateTime = date('dmY-His');
  $fileName = $dateTime . '-' . $projectName . '.json';
  $filePath = './' . $fileName;

  // Create an array with project_name, comment and imageBase64 values
  $data = array(
    'project_name' => $projectName,
    'comment' => $comment,
    'imageBase64' => $imageBase64
  );

  // Write the data to a JSON file
  file_put_contents($filePath, json_encode($data, JSON_UNESCAPED_UNICODE));

  echo 'Data successfully saved to ' . $fileName;
}


?>
