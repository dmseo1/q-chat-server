<?php
	/**
	 * ERROR_CODE_0 : 첫 번째 쿼리 실패
	 * ERROR_CODE_1 : 첫 번째 결과 비정상
	 */
	include("../dbconn/dbconn.php");
	$query = "SELECT * FROM characters";
	$data = array();
	$result = pg_exec($DBCONN, $query) or die("ERROR_CODE_0");
	if($result) {
		while($row = pg_fetch_assoc($result)) {
			array_push($data, array(
				'character_no' => $row['character_no'],
				'path' => $row['path']
			));
		}
			
		header('Content-Type: application/json; charset=utf8');
		$json = json_encode(array("characters_list"=>$data));
		echo $json;
	} else {
		echo "ERROR_CODE_1"; return;
	}
?>