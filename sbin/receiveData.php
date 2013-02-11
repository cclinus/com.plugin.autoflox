<?php

# This script is used to receive json data from autoflox java program, and send it to java socket server.

$data = file_get_contents("php://input");

$file = fopen("dump_data","a");
echo fwrite($file,$data);
fclose($file);

chmod("dump_data", 0777);

?>
