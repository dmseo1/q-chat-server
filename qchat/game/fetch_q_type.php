<?php
	include("../dbconn/dbconn.php");
	$query = "SELECT * from q_type";
	$result = pg_exec($DBCONN, $query) or die("ERROR_CODE_0");
	$data = array();
	if($result) {
		while($row = pg_fetch_assoc($result)) {
			array_push($data,
				$row['q_type']);
		}
		header('Content-Type: application/json; charset=utf8');
		$json = json_encode(array("q_type"=>$data));
		echo $json;
	} else {
		echo "ERROR_CODE_1";
	}
?>