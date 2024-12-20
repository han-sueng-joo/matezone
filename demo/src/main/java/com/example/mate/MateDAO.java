package com.example.mate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import java.sql.Statement;

@Component
public class MateDAO {
	final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	final String JDBC_URL = "jdbc:mysql://localhost/mate?allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC";
	

	// DB 연결을 가져오는 메서드, DBCP를 사용하는 것이 좋음
	public Connection open() {
		Connection conn = null;
		try {
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(JDBC_URL,"root","1234");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
	}

	public List<Post> getAll() throws Exception {
		Connection conn = open();
		List<Post> postList = new ArrayList<>();
		String sql = "SELECT title FROM post";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		ResultSet rs = pstmt.executeQuery();
		try (conn; pstmt; rs) {
			while (rs.next()) {
				Post n = new Post();
				n.setTitle(rs.getString("title"));
				postList.add(n);
			}
			return postList;
		}
	}

	public void addPost(Post n, List<Integer> tagIds) throws Exception {
		Connection conn = open();
		String postSql = "INSERT INTO post(title, img, createdAt, content) VALUES (?, ?, CURRENT_TIMESTAMP(), ?)";
		String postTagSql = "INSERT INTO posttag(tagId, postId) VALUES (?, ?)";
	
		PreparedStatement postPstmt = conn.prepareStatement(postSql, Statement.RETURN_GENERATED_KEYS);
		PreparedStatement postTagPstmt = conn.prepareStatement(postTagSql);

		try (conn; postPstmt; postTagPstmt) {
			// 1. Post 테이블에 데이터 삽입
			postPstmt.setString(1, n.getTitle());
			postPstmt.setString(2, n.getImg());
			postPstmt.setString(3, n.getContent());
			postPstmt.executeUpdate();

			// 2. 생성된 postId 가져오기
			ResultSet rs = postPstmt.getGeneratedKeys();
			int postId = 0;
			if (rs.next()) {
				postId = rs.getInt(1); // 생성된 postId 가져오기
			}

			// 3. Posttag 테이블에 tagId와 postId 삽입
			for (int tagId : tagIds) {
				postTagPstmt.setInt(1, tagId);
				postTagPstmt.setInt(2, postId);
				postTagPstmt.addBatch(); // 배치에 추가
			}
			postTagPstmt.executeBatch(); // 배치 실행
		}
	}

	public Post getPost(int aid) throws SQLException {
		Connection conn = open();
		Post n = new Post();
		String sql = "SELECT title, img, createdAt, content FROM post WHERE aid = ?";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		pstmt.setInt(1, aid);// 1은 ?의 위치 / 위치 정보는 1부터 시작
		ResultSet rs = pstmt.executeQuery();
		rs.next();

		try (conn; pstmt; rs) {
			n.setPostId(rs.getInt("aid"));
			n.setTitle(rs.getString("title"));
			n.setImg(rs.getString("img"));
			n.setCreatedAt(rs.getString("createdAt"));
			n.setContent(rs.getString("content"));
			pstmt.executeQuery();
			return n;
		}
	}

	public void delPost(int aid) throws SQLException {
		Connection conn = open();
		String sql = "delete from news where aid=?";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		try (conn; pstmt) {
			pstmt.setInt(1, aid);
			// 삭제된 게스글이 없을 경우
			if (pstmt.executeUpdate() == 0) {
				throw new SQLException("DB에러");
			}
		}
	}

	public void updatePost(Post n) throws SQLException {
		// DB 연결
		Connection conn = open();
		String sql = "UPDATE news SET title = ?, content = ?, img = ? WHERE aid = ?";
		PreparedStatement pstmt = conn.prepareStatement(sql);

		try (conn; pstmt) {
			// PreparedStatement에 값을 설정
			pstmt.setString(1, n.getTitle()); // 제목
			pstmt.setString(2, n.getContent()); // 내용
			pstmt.setString(3, n.getImg()); // 이미지 경로
			pstmt.setInt(4, n.getPostId()); // 게시글 ID

			// UPDATE 쿼리 실행
			pstmt.executeUpdate();
		}
	}
 
}
