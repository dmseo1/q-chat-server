<?php
	include("./dbconn/dbconn.php");
	$query = "SELECT * FROM variables";
	$result = pg_exec($DBCONN, $query) or die("ERROR_CODE_0");
	$data = array();
	$value = array();
	if($result) {
		while($row = pg_fetch_assoc($result)) {
			$data[$row['key']] = $row['value'];
		}
		header('Content-Type: application/json; charset=utf8');
		$json = json_encode(array("app_info"=>$data));
		echo $json;
	} else {
		echo "ERROR_CODE_1";
	}
?>