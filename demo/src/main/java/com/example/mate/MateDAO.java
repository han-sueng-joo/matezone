package com.example.mate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

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
    /*
     * 
     * 
     * public void addPost(Post n) throws Exception {
		Connection conn = open();
		String sql = "insert into post(title,img,date,content) values(?,?,CURRENT_TIMESTAMP(),?)";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		try (conn; pstmt) {// try 블록이 종료 될때 conn과 pstmt는 자동으로 닫히는 리소스로 간주
			pstmt.setString(1, n.getTitle());
			pstmt.setString(2, n.getImg());
			pstmt.setString(3, n.getContent());
			pstmt.executeUpdate();
		}

		
	}

	public News getNews(int aid) throws SQLException {
		Connection conn = open();
		News n = new News();
		String sql = "select aid, title, img, PARSEDATETIME(FORMATDATETIME(date, 'yyyy-MM-dd HH:mm:ss'), 'yyyy-MM-dd HH:mm:ss') as cdate, content from news where aid=?";
		//mysql문법에 맞춰 sql문이 수정됨
		//String sql = "SELECT aid, title, img, DATE_FORMAT(date, '%Y-%m-%d %H:%i:%s') AS cdate, content FROM news WHERE aid = ?";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		pstmt.setInt(1, aid);// 1은 ?의 위치 / 위치 정보는 1부터 시작
		ResultSet rs = pstmt.executeQuery();
		rs.next();

		try (conn; pstmt; rs) {
			n.setAid(rs.getInt("aid"));
			n.setTitle(rs.getString("title"));
			n.setImg(rs.getString("img"));
			n.setDate(rs.getString("cdate"));
			n.setContent(rs.getString("content"));
			pstmt.executeQuery();
			return n;
		}
	}

	public void delNews(int aid) throws SQLException {
		Connection conn = open();
		String sql = "delete from news where aid=?";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		try (conn; pstmt) {
			pstmt.setInt(1, aid);
			// 삭제된 뉴스 기사가 없을 경우
			if (pstmt.executeUpdate() == 0) {
				throw new SQLException("DB에러");
			}
		}
	}

	public void updateNews(News n) throws SQLException {
		// DB 연결
		Connection conn = open();
		String sql = "UPDATE news SET title = ?, content = ?, img = ? WHERE aid = ?";
		PreparedStatement pstmt = conn.prepareStatement(sql);

		try (conn; pstmt) {
			// PreparedStatement에 값을 설정
			pstmt.setString(1, n.getTitle()); // 제목
			pstmt.setString(2, n.getContent()); // 내용
			pstmt.setString(3, n.getImg()); // 이미지 경로
			pstmt.setInt(4, n.getAid()); // 뉴스 ID (aid)

			// UPDATE 쿼리 실행
			pstmt.executeUpdate();
        }
    }
     * 
     * 
     */
	
}
