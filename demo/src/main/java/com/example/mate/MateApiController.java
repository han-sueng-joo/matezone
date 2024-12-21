package com.example.mate;

import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MateApiController {
	final MateDAO dao;

	@Autowired
	public MateApiController(MateDAO dao) {
		this.dao = dao;
	}

    // 게시글 목록
	@GetMapping("/post")
	public List<Post> getPostList() {
		List<Post> postList = null;
		try {
			postList = dao.getPostAll();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return postList;
	}

	@PostMapping
	public String addPost(@RequestBody Post post, @RequestParam("tagName") List<String> tagName) {
		try {
			dao.addPost(post, tagName);
		} catch (Exception e) {
			e.printStackTrace();
			return "Post API: 게시글 등록 실패!!";
		}
		return "Post API: 게시글 등록됨!!";
	}

	@DeleteMapping("{aid}")
	public String delPost(@PathVariable("aid") int aid) {
		try {
			dao.delPost(aid);
		} catch (SQLException e) {
			e.printStackTrace();
			return "News API: 뉴스 삭제 실패!! -" + aid;
		}
		return "News API: 뉴스 삭제됨!! -" + aid;
	}

	@GetMapping("{aid}")
	public Post getPost(@PathVariable("aid") int aid) {
		Post post = null;
		try {
			post = dao.getPost(aid);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return post;
	}
}