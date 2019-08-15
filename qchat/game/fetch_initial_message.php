<?php
	include("../dbconn/dbconn.php");
	$query = "SELECT * from variables where key LIKE 'chatting_room_init_%'";
	$result = pg_exec($DBCONN, $query) or die("ERROR_CODE_0");
	$data = array();
	if($result) {
		$i = 0;
		while($row = pg_fetch_assoc($result)) {
			$data[$i . ''] = $row['value'];
			$i ++; 
		}
		header('Content-Type: application/json; charset=utf8');
		$json = json_encode(array($data));
		echo $json;
	} else {
		echo "ERROR_CODE_1";
	}
?>