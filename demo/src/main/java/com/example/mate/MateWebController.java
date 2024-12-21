package com.example.mate;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/mate")
public class MateWebController {
	final MateDAO dao;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
    // 프로퍼티 파일의 저장 경로
	@Value("${post.imgdir}")
	String fdir;

	@Autowired
	public MateWebController(MateDAO dao) {
		this.dao = dao;
	}

    @GetMapping("/")
	public String listPost(Model m) {
		List<Post> list;
		List<Tag> tagName;
		try {
			list = dao.getPostAll();
			tagName = dao.getAllTags();
			m.addAttribute("postlist", list);
			m.addAttribute("tagName", tagName);
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("게시글 목록 또는 테그 생성 과정에서 문제 발생!!");
			m.addAttribute("error", "게시글 목록 또는 테그가 정상적으로 처리되지 않았습니다!!");
		}
		return "MAIN";
	}

	@PostMapping("/add")
	public String addPost(@ModelAttribute Post post, Model m, @RequestParam("image") MultipartFile image, @RequestParam(value = "attachment", required = false) MultipartFile attachment,
	@RequestParam("tags") String tags) {
		try {
			// 파일 저장 처리
			if (!image.isEmpty()) {
				File dest = new File(fdir + "/" + image.getOriginalFilename());
				image.transferTo(dest);
				post.setImg("/img/" + dest.getName());
			}
	
			if (attachment != null && !attachment.isEmpty()) {
				File attachDest = new File(fdir + "/" + attachment.getOriginalFilename());
				attachment.transferTo(attachDest);
			}
	
			// 태그 처리
			List<String> tagList = Arrays.asList(tags.split(","));
			dao.addPost(post, tagList);
		} catch (Exception e) {
			logger.error("게시글 추가 중 오류 발생", e);
			m.addAttribute("error", "게시글 등록 실패");
		}
		return "redirect:/mate/";
	}

	@GetMapping("/deletePost/{postId}")
	public String deletePost(@PathVariable("postId") int postId, Model m) {
		try {
			dao.delPost(postId);
			return "redirect:/mate/"; // 삭제 후 카테고리 뷰로 이동
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("게시글 삭제 과정에서 문제 발생!");
			m.addAttribute("error", "게시글 삭제에 실패했습니다.");
			return "postDetail";
		}
	}

	@GetMapping("/postDetail/{postId}")
	public String getPost(@PathVariable("postId") int postId, Model m) {
		System.out.println("Request received for postDetail with ID: " + postId);
		try {
			System.out.println("Requested post ID: " + postId); // 디버깅 로그
	
			// 게시글 가져오기
			Post n = dao.getPost(postId);
	
			// 게시글이 없을 경우
			if (n == null) {
				System.err.println("No post found with ID: " + postId);
				m.addAttribute("error", "게시글이 존재하지 않습니다!");
				return "error"; // 에러 페이지로 리다이렉트
			}
			List<Tag> t = dao.getTagsByPost(postId);
	
			// 게시글을 모델에 추가
			m.addAttribute("post", n);
			m.addAttribute("tags", t);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.warn("게시글을 가져오는 과정에서 문제 발생!!");
	
			// SQL 예외 정보 출력
			System.err.println("SQL Error: " + e.getMessage());
			m.addAttribute("error", "데이터베이스 오류로 인해 게시글을 가져올 수 없습니다.");
			return "error"; // 에러 페이지로 리다이렉트
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Unexpected error occurred: ", e);
	
			// 예기치 않은 예외 처리
			System.err.println("Unexpected Error: " + e.getMessage());
			m.addAttribute("error", "예기치 못한 오류가 발생했습니다.");
			return "error"; // 에러 페이지로 리다이렉트
		}
		return "postDetail"; // 정상적으로 게시글 세부 정보 페이지로 이동
	}

	@GetMapping("login")
	public String login(Model m) {		
		return "login";
	}

	@GetMapping("/write")
	public String write(Model m) {
		return "write";
	}

	@GetMapping("/editPost/{postId}")
	public String editPost(@PathVariable("postId") int postId, Model m) {
		try {
			// 게시글 데이터 로드
			Post post = dao.getPost(postId);
			List<Tag> t = dao.getTagsByPost(postId);
			m.addAttribute("post", post);
			m.addAttribute("tags", t);
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("게시글 데이터를 불러오는 과정에서 문제 발생!");
			m.addAttribute("error", "게시글을 불러오지 못했습니다.");
		}
		return "editPost"; // 수정 페이지 템플릿으로 이동
	}
	
	@PostMapping("/editPost")
	public String savePost(@ModelAttribute Post post, Model m) {
		try {
			// 수정된 게시글 데이터를 DB에 저장
			dao.updatePost(post);
			return "redirect:/mate/postDetail/" + post.getPostId(); // 수정 완료 후 상세 페이지로 이동
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("게시글 수정 과정에서 문제 발생!");
			m.addAttribute("error", "게시글을 저장하지 못했습니다.");
			return "editPost"; // 에러 발생 시 다시 수정 페이지로 이동
		}
	}

	@GetMapping("/categoryView/{categoryId}")
	public String categoryView(@PathVariable("categoryId") int categoryId, Model m) {
		List<Post> list;
		List<Tag> tagName;
		try {
			list = dao.getDistinctTitlesByCategory(categoryId);
			tagName = dao.getTagsByCategory(categoryId);
			m.addAttribute("postlist", list);
			m.addAttribute("tagName", tagName);
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("게시글 목록 또는 테그 생성 과정에서 문제 발생!!");
			m.addAttribute("error", "게시글 목록 또는 테그가 정상적으로 처리되지 않았습니다!!");
		}

		return "categoryView";
	}
}