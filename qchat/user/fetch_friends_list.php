<?php
/**
 * ERROR_CODE_0 : 첫 번째 쿼리 실패
 * ERROR_CODE_1 : 요청한 유저 정보 확인 실패
 * ERROR_CODE_2 : 두 번째 쿼리 실패
 * ERROR_CODE_3 : 요청한 유저의 친구 리스트 불러오기 실패 
 */

	include("../dbconn/dbconn.php");
	$user_no = $_POST['user_no'];
	$query = "SELECT * from users where user_no='$user_no'";
	$result = pg_exec($DBCONN, $query) or die("ERROR_CODE_0");
	if($result) {
		while($row = pg_fetch_assoc($result)) {
			$friends_no_string = $row['user_friends_list'];
			if(!strcmp($friends_no_string, '')) echo "EMPTY_LIST";
			else {
				$friends_no_array = explode(",", $friends_no_string);
				$query2 = "SELECT * FROM users WHERE user_no=" . $friends_no_array[0];
				for($i = 1; $i < count($friends_no_array); $i ++) {
					$query2 .= ' OR user_no=' . $friends_no_array[$i];
				}

				$result2 = pg_exec($DBCONN, $query2) or die("ERROR_CODE_2");
				if($result2) {
					$data = array();
					while($row = pg_fetch_assoc($result2)) {
						array_push($data,
							array(
							'user_no' => $row['user_no'],
							'user_id' => $row['user_id'],
							'user_email' => $row['user_email'],
							'user_nickname' => $row['user_nickname'],
							'user_status_message' => $row['user_status_message'],
							'user_profile_img_path_t' => $row['user_profile_img_path_t'],
							));
					}
					header('Content-Type: application/json; charset=utf8');
					$json = json_encode(array("friends_list"=>$data));
					echo $json;
				} else {
					echo "ERROR_CODE_3";
				}
			}
		}
	} else {
		echo "ERROR_CODE_1";
	}
?>