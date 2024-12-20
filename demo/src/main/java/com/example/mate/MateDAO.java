package com.example.mate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

	public List<Post> getPostAll() throws Exception {
		Connection conn = open();
		List<Post> postList = new ArrayList<>();
		String sql = "SELECT title, postId FROM post";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		ResultSet rs = pstmt.executeQuery();
		try (conn; pstmt; rs) {
			while (rs.next()) {
				Post n = new Post();
				n.setTitle(rs.getString("title"));
				n.setPostId(rs.getInt("postId"));
				postList.add(n);
			}
			return postList;
		}
	}

	public List<Post> getDistinctTitlesByCategory(int categoryId) throws Exception {
		Connection conn = open();
        List<Post> titles = new ArrayList<>();
        String sql = """
                SELECT DISTINCT 
                    p.title, p.postId
                FROM 
                    post p
                JOIN 
                    posttag pt ON p.postId = pt.postId
                JOIN 
                    tag t ON pt.tagId = t.tagId
                WHERE 
                    t.categoryId = ?
                """;
		
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, categoryId); // categoryId를 바인딩
	
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					Post post = new Post(); // Post 객체 생성
					post.setTitle(rs.getString("title")); // 제목 설정
					post.setPostId(rs.getInt("postId")); // postId 설정 (수정)
					titles.add(post); // 리스트에 Post 객체 추가
				}
			}
		}

        return titles;
    }

	public void addPost(Post n, List<String> tagNames, String userId) throws Exception {
		Connection conn = open();

		String postSql = "INSERT INTO post(title, img, createdAt, content, userId) VALUES (?, ?, CURRENT_TIMESTAMP(), ?, ?)";
		String getTagIdSql = "SELECT tagId FROM tag WHERE tagName = ?";
		String postTagSql = "INSERT INTO posttag(tagId, postId) VALUES (?, ?)";

		try (PreparedStatement postPstmt = conn.prepareStatement(postSql, Statement.RETURN_GENERATED_KEYS);
					PreparedStatement getTagIdPstmt = conn.prepareStatement(getTagIdSql);
					PreparedStatement postTagPstmt = conn.prepareStatement(postTagSql)) {

				// 1. Post 테이블에 데이터 삽입
				postPstmt.setString(1, n.getTitle());
				postPstmt.setString(2, n.getImg());
				postPstmt.setString(3, n.getContent());
				postPstmt.setString(4, userId != null ? userId : "defaultUserId");
				postPstmt.executeUpdate();

				// 2. 생성된 postId 가져오기
				int postId;
				try (ResultSet rs = postPstmt.getGeneratedKeys()) {
						if (rs.next()) {
								postId = rs.getInt(1);
						} else {
								throw new SQLException("Failed to retrieve generated postId.");
						}
				}

				// 3. 태그 이름으로 `tagId`를 조회하고, `posttag`에 삽입
				// 태그 처리
				for (String tagName : tagNames) {
					// 해시태그 제거 후 검색
					String processedTagName = tagName.startsWith("#") ? tagName.substring(1) : tagName;
					try {
						getTagIdPstmt.setString(1, processedTagName);
						ResultSet tagRs = getTagIdPstmt.executeQuery();

						if (tagRs.next()) {
							int tagId = tagRs.getInt("tagId"); // 태그 ID 가져오기

							// Posttag 테이블에 삽입
							postTagPstmt.setInt(1, tagId);
							postTagPstmt.setInt(2, postId);
							postTagPstmt.addBatch(); // 배치에 추가
						} else {
							System.err.println("Tag not found: " + processedTagName);
						}
					} catch (SQLException e) {
							System.err.println("Error retrieving tagId for tagName: " + processedTagName);
							throw e;
					}
				}

				// 4. Batch 실행
				postTagPstmt.executeBatch();

				} catch (Exception e) {
						System.err.println("Error while adding post and tags");
						e.printStackTrace();
						throw e;
				}
	}

	public Post getPost(int postId) throws SQLException {
		Connection conn = open();
		Post n = new Post();
		String sql = "SELECT postId, title, img, createdAt, content, userId, applyNum FROM post WHERE postId = ?";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		pstmt.setInt(1, postId);// 1은 ?의 위치 / 위치 정보는 1부터 시작
		ResultSet rs = pstmt.executeQuery();
		

		try (conn; pstmt; rs) {
			if (rs.next()) {
				n.setPostId(rs.getInt("postId"));
				n.setTitle(rs.getString("title"));
				n.setImg(rs.getString("img"));
				n.setCreatedAt(rs.getString("createdAt"));
				n.setContent(rs.getString("content"));
				n.setUserId(rs.getString("userId"));
				n.setApplyNum(rs.getInt("applyNum"));
			} else {
				throw new SQLException("No post found with postId: " + postId);
			}
			
			//pstmt.executeQuery();
			return n;
		}
	}

	public void delPost(int postId) throws SQLException {
		Connection conn = null;
		PreparedStatement deletePostTagStmt = null;
		PreparedStatement deletePostStmt = null;
		try {
			conn = open();
	
			// 1. posttag 테이블의 참조 데이터 삭제
			String deletePostTagSql = "DELETE FROM posttag WHERE postId = ?";
			deletePostTagStmt = conn.prepareStatement(deletePostTagSql);
			deletePostTagStmt.setInt(1, postId);
			deletePostTagStmt.executeUpdate();
	
			// 2. post 테이블의 데이터 삭제
			String deletePostSql = "DELETE FROM post WHERE postId = ?";
			deletePostStmt = conn.prepareStatement(deletePostSql);
			deletePostStmt.setInt(1, postId);
			deletePostStmt.executeUpdate();
	
		} finally {
			if (deletePostTagStmt != null) deletePostTagStmt.close();
			if (deletePostStmt != null) deletePostStmt.close();
			if (conn != null) conn.close();
		}
	}

	public void updatePost(Post post) throws SQLException {
		String sql = """
				UPDATE post 
				SET title = ?, img = ?, content = ? 
				WHERE postId = ?
				""";
	
		try (Connection conn = open();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, post.getTitle());
			pstmt.setString(2, post.getImg());
			pstmt.setString(3, post.getContent());
			pstmt.setInt(4, post.getPostId());
	
			int updatedRows = pstmt.executeUpdate();
			if (updatedRows == 0) {
				throw new SQLException("No post found with postId: " + post.getPostId());
			}
		}
	}
 
	public List<Tag> getTagsByCategory(int categoryId) throws Exception {
		Connection conn = open();
        List<Tag> tagNames = new ArrayList<>();
        String sql = """
                SELECT tagName 
                FROM tag
                WHERE categoryId = ? OR categoryId = 5
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
					Tag tag = new Tag();
					tag.setTagName(rs.getString("tagName"));
					tagNames.add(tag);
                }
            }
        }
        return tagNames;
    }

	public List<Tag> getAllTags() throws Exception {
		Connection conn = open();
        List<Tag> tagNames = new ArrayList<>();
        String sql = "SELECT tagName FROM tag";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Tag tag = new Tag();
				tag.setTagName(rs.getString("tagName"));
				tagNames.add(tag);
            }
        }
        return tagNames;
    }

	public List<Tag> getTagsByPost(int postId) throws Exception {
		List<Tag> tags = new ArrayList<>();
		String sql = """
				SELECT t.tagId, t.tagName
				FROM posttag pt
				INNER JOIN tag t ON pt.tagId = t.tagId
				WHERE pt.postId = ?
				""";
	
		try (Connection conn = open();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, postId);
	
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					Tag tag = new Tag();
					tag.setTagId(rs.getInt("tagId")); // 태그 ID 설정
					tag.setTagName(rs.getString("tagName")); // 태그 이름 설정
					tags.add(tag);
				}
			}
		}
		return tags;
	}

	public boolean authenticate(String email, String password) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = open();
			String sql = "SELECT password FROM user WHERE userId = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, email);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				String storedPassword = rs.getString("password");
				return storedPassword.equals(password);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) rs.close();
				if (pstmt != null) pstmt.close();
				if (conn != null) conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return false;
    }

	public boolean isEmailDuplicated(String email) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = open();
			String sql = "SELECT userId FROM user WHERE userId = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, email);
			rs = pstmt.executeQuery();
	
			return rs.next(); // 중복된 이메일이 있으면 true 반환
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) rs.close();
				if (pstmt != null) pstmt.close();
				if (conn != null) conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return false; // 중복된 이메일이 없으면 false 반환
	}

	public void insertUser(String userName, String userId, String password, String grade, String sex) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = open();
            String sql = "INSERT INTO user (userName, userId, password, grade, sex) VALUES (?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userName);
            pstmt.setString(2, userId);
            pstmt.setString(3, password);
            pstmt.setString(4, grade);
            pstmt.setString(5, sex);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        }
    }

	// Increment applyNum for a given postId
    public void incrementApplyNum(int postId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = open();
            String sql = "UPDATE post SET applyNum = applyNum + 1 WHERE postId = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        }
    }

    // Add a user-post relation
    public void addUserPost(String userId, int postId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = open();
            String sql = "INSERT INTO userpost (userId, postId) VALUES (?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            pstmt.setInt(2, postId);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        }
    }

	// Check if user already applied to the post
    public boolean isUserAlreadyApplied(String userId, int postId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = open();
            String sql = "SELECT 1 FROM userpost WHERE userId = ? AND postId = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            pstmt.setInt(2, postId);
            rs = pstmt.executeQuery();
            return rs.next();
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        }
    }

	public void decrementApplyNum(int postId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = open();
            String sql = "UPDATE post SET applyNum = applyNum - 1 WHERE postId = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        }
    }

	public void delUserPost(String userId, int postId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = open();
            String sql = "DELETE FROM userpost WHERE userId = ? AND postId = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            pstmt.setInt(2, postId);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        }
    }

	public List<Post> getPostsByTag(String tagName) throws Exception {
		Connection conn = open();
		List<Post> posts = new ArrayList<>();
		String sql = """
						SELECT p.postId, p.title
						FROM post p
						JOIN posttag pt ON p.postId = pt.postId
						JOIN tag t ON pt.tagId = t.tagId
						WHERE t.tagName = ?
						""";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setString(1, tagName);
				try (ResultSet rs = pstmt.executeQuery()) {
						while (rs.next()) {
								Post post = new Post();
								post.setPostId(rs.getInt("postId"));
								post.setTitle(rs.getString("title"));
								posts.add(post);
						}
				}
		}
		return posts;
}
}
