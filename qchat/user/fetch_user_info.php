<?php
	include("../dbconn/dbconn.php");
	$user_no = $_POST['user_no'];
	$query = "SELECT * from users where user_no='$user_no'";
	$result = pg_exec($DBCONN, $query) or die("ERROR");
	$data = array();
	if($result) {
		while($row = pg_fetch_assoc($result)) {
			array_push($data,
				array(
				'user_no' => $row['user_no'],
				'user_id' => $row['user_id'],
				'user_email' => $row['user_email'],
				'user_nickname' => $row['user_nickname'],
				'user_status_message' => $row['user_status_message'],
				'user_point' => $row['user_point'],
				'user_exp' => $row['user_exp'],
				'user_now_using_character' => $row['user_now_using_character'],
				'user_profile_img_path' => $row['user_profile_img_path'],
				'user_profile_img_path_t' => $row['user_profile_img_path_t'],
				'user_message_block_list' => $row['user_message_block_list'],
				'user_friends_list' => $row['user_friends_list'],
				'user_characters_list' => $row['user_characters_list']
				));
		}
		header('Content-Type: application/json; charset=utf8');
		$json = json_encode(array("user_info"=>$data));
		echo $json;
	} else {
		echo "ERROR";
	}
?>