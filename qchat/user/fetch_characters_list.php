<?php
	/**
	 * ERROR_CODE_0 : 첫 번째 쿼리 실패
	 * ERROR_CODE_1 : 첫 번째 결과 비정상
	 * ERROR_CODE_2 : 두 번째 쿼리 실패
	 * ERROR_CODE_3 : 두 번째 결과 비정상
	 */
	include("../dbconn/dbconn.php");
	$user_no = $_POST['user_no'];
	$query = "SELECT user_characters_list FROM users WHERE user_no='$user_no'";
	$characters_no_list = "";
	$result = pg_exec($DBCONN, $query) or die("ERROR_CODE_0");
	if($result) {
		while($row = pg_fetch_assoc($result)) {
			$characters_no_list = $row['user_characters_list'];
		}
		if(!strcmp($characters_no_list, '')) echo "EMPTY_LIST"; //This should not be happened!
		else {
			$characters_no_array = explode(',', $characters_no_list);
			$query2 = "SELECT * FROM characters WHERE character_no=" . $characters_no_array[0];
			for($i = 1; $i < count($characters_no_array); $i ++) {
				$query2 .= " OR character_no= " . $characters_no_array[$i];
			}
			$result2 = pg_exec($DBCONN, $query2) or die("ERROR_CODE_2");
			$data = array();
			if($result2) {
				while($row = pg_fetch_assoc($result2)) {
					array_push($data,
						array(
						'character_no' => $row['character_no'],
						'character_name' => $row['character_name'],
						'is_facable' => $row['is_facable'],
						'face_pos_x' => $row['face_pos_x'],
						'face_pos_y' => $row['face_pos_y'],
						'face_radius' => $row['face_radius'],
						'path' => $row['path'],
					));
				}
				header('Content-Type: application/json; charset=utf8');
				$json = json_encode(array("characters_list"=>$data));
				echo $json;
			} else {
				echo "ERROR_CODE_3";
			}
		}
	} else {
		echo "ERROR_CODE_1"; return;
	}
?>