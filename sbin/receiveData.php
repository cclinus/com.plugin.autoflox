<?php

# This script is used to receive json data from autoflox java program, and send it to java socket server.

$data = file_get_contents("php://input");
$fileName = "dumpTrace/dump_data_".microtime(true);
file_put_contents($fileName, $data, FILE_APPEND | LOCK_EX);
chmod($fileName, 0777);

// For debugging log

$data .= "\n\n\n*****************\n\n\n";
file_put_contents("dump_data_copy", $data, FILE_APPEND | LOCK_EX);
chmod("dump_data", 0777);

?>
