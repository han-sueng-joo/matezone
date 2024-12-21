package com.example.mate;

import java.io.File;
import java.sql.PreparedStatement;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

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
	public String listPost(HttpSession session, Model m) {
		List<Post> list;
		List<Tag> tagName;
		try {
			list = dao.getPostAll();
			tagName = dao.getAllTags();
			m.addAttribute("postlist", list);
			m.addAttribute("tagName", tagName);

			// 세션에서 userId 가져오기
			String userId = (String) session.getAttribute("userId");
			
			m.addAttribute("userId", userId); // userId가 있으면 모델에 추가
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("게시글 목록 또는 테그 생성 과정에서 문제 발생!!");
			m.addAttribute("error", "게시글 목록 또는 테그가 정상적으로 처리되지 않았습니다!!");
		}
		return "MAIN";
	}

	@PostMapping("/add")
	public String addPost(@ModelAttribute Post post, Model m,
						@RequestParam("image") MultipartFile image,
						@RequestParam(value = "attachment", required = false) MultipartFile attachment,
						@RequestParam("tags") String tags,
						HttpSession session) {
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
			logger.info("Received tags: {}", tagList);

			// 세션에서 userId 가져오기
			String userId = (String) session.getAttribute("userId");
			logger.info("Retrieved userId from session: {}", userId);

			// 게시글 및 태그 저장
			dao.addPost(post, tagList, userId);

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
	public String getPost(@PathVariable("postId") int postId, HttpSession session, Model m) {
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
			// 세션에서 userId 가져오기
			String userId = (String) session.getAttribute("userId");
			boolean isDupul = dao.isUserAlreadyApplied(userId, postId);
			
			m.addAttribute("userId", userId); // userId가 있으면 모델에 추가
			m.addAttribute("isDupul", isDupul);
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

	@PostMapping("/signin")
	public String saigin(@RequestParam("userId") String userId, @RequestParam("password") String password, HttpSession session, Model model) {
		if (dao.authenticate(userId, password)) {
			session.setAttribute("userId", userId); // 세션에 사용자 정보 저장
			// 로그인 성공
			return "redirect:/mate/";
		} else {
			// 로그인 실패
			model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
			return "login"; // 로그인 페이지로 다시 이동
		}
	}

	@PostMapping("/signup")
	public String saigup(@RequestParam("userName") String userName, 
						@RequestParam("userId") String userId, 
						@RequestParam("password") String password, 
						@RequestParam("grade") String grade, 
						@RequestParam("sex") String sex, 
						Model model) {
		if (dao.isEmailDuplicated(userId)) {
			// 이메일 중복 시 처리
			model.addAttribute("error", "이미 사용 중인 이메일입니다.");
			return "signup"; // 회원가입 페이지로 다시 이동
		} else {
			try {
				dao.insertUser(userName, userId, password, grade, sex);
			} catch (Exception e) {
				logger.error("회원가입 중 오류 발생: ", e);
				model.addAttribute("error", "회원가입 중 오류가 발생했습니다.");
				return "signup";
			}
			return "redirect:/mate/login"; // 로그인 페이지로 리다이렉트
		}
	}

	@GetMapping("/login")
	public String login(Model m) {
		return "login";
	}

	@GetMapping("/write")
	public String write(HttpSession session, Model m) {
			String userId = (String) session.getAttribute("userId"); // 세션에서 사용자 정보 가져오기
		if (userId == null) {
			return "redirect:/mate/login"; // 비로그인 사용자는 로그인 페이지로 리다이렉트
		}
		m.addAttribute("userId", userId); // 로그인 사용자 정보 추가
		return "write";
	}

	@GetMapping("/editPost/{postId}")
	public String editPost(@PathVariable("postId") int postId, HttpSession session, Model m) {
		try {
			// 게시글 데이터 로드
			Post post = dao.getPost(postId);
			String userId = (String) session.getAttribute("userId");

			if (post == null || post.getUserId() == null) {
				m.addAttribute("error", "게시글을 찾을 수 없습니다.");
				return "redirect:/mate/";
			}
			
			if (userId == null || !userId.equals(post.getUserId())) {
				m.addAttribute("error", "권한이 없습니다.");
				return "redirect:/mate/postDetail/" + postId; // 권한 없을 때 해당 사용자 페이지로 리다이렉트
			}

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
	public String categoryView(@PathVariable("categoryId") int categoryId, HttpSession session, Model m) {
		List<Post> list;
		List<Tag> tagName;
		try {
			list = dao.getDistinctTitlesByCategory(categoryId);
			tagName = dao.getTagsByCategory(categoryId);
			m.addAttribute("postlist", list);
			m.addAttribute("tagName", tagName);
			// 세션에서 userId 가져오기
			String userId = (String) session.getAttribute("userId");
			
			m.addAttribute("userId", userId); // userId가 있으면 모델에 추가
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("게시글 목록 또는 테그 생성 과정에서 문제 발생!!");
			m.addAttribute("error", "게시글 목록 또는 테그가 정상적으로 처리되지 않았습니다!!");
		}

		return "categoryView";
	}

	@GetMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate(); // 세션 무효화
		return "redirect:/mate/"; // 로그아웃 후 로그인 페이지로 이동
	}

	@GetMapping("/apply/{postId}")
	public String apply(RedirectAttributes redirectAttributes, HttpSession session, @PathVariable("postId") int postId, Model m) {
		try {
			// 로그인 여부 확인
			String userId = (String) session.getAttribute("userId");
			if (userId == null) {
				m.addAttribute("error", "로그인이 필요합니다.");
				return "redirect:/mate/login";
			}

			// 게시글 정보 가져오기
			Post post = dao.getPost(postId);
			if (post.getUserId().equals(userId)) {
				m.addAttribute("error", "자신의 게시글에는 신청할 수 없습니다.");
				return "redirect:/mate/postDetail/" + postId;
			}
			
			//중복체크
			if (dao.isUserAlreadyApplied(userId, post.getPostId())) {
				redirectAttributes.addFlashAttribute("error", "게시글에 중복 신청할 수 없습니다.");
				return "redirect:/mate/postDetail/" + postId;
			}

			// applyNum 증가
			dao.incrementApplyNum(postId);

			// userpost 테이블에 데이터 추가
			dao.addUserPost(userId, postId);

		} catch (Exception e) {
			e.printStackTrace();
			m.addAttribute("error", "신청 처리 중 오류가 발생했습니다.");
			return "redirect:/mate/postDetail/" + postId;
		}

		return "redirect:/mate/postDetail/" + postId;
	}

	@GetMapping("/applyCancel/{postId}")
	public String applyCancel(RedirectAttributes redirectAttributes, HttpSession session, @PathVariable("postId") int postId, Model m) {
		try {
			// 로그인 여부 확인
			String userId = (String) session.getAttribute("userId");
			if (userId == null) {
				m.addAttribute("error", "로그인이 필요합니다.");
				return "redirect:/mate/login";
			}

			// 게시글 정보 가져오기
			Post post = dao.getPost(postId);
			if (post.getUserId().equals(userId)) {
				m.addAttribute("error", "자신의 게시글에는 신청할 수 없습니다.");
				return "redirect:/mate/postDetail/" + postId;
			}
			
			//신청체크
			if (!dao.isUserAlreadyApplied(userId, post.getPostId())) {
				redirectAttributes.addFlashAttribute("error", "신청하지 않은 게시글을 취소할 수 없습니다.");
				return "redirect:/mate/postDetail/" + postId;
			}

			// applyNum 감소
			dao.decrementApplyNum(postId);

			// userpost 테이블에 데이터 삭제
			dao.delUserPost(userId, postId);

		} catch (Exception e) {
			e.printStackTrace();
			m.addAttribute("error", "신청 처리 중 오류가 발생했습니다.");
			return "redirect:/mate/postDetail/" + postId;
		}

		return "redirect:/mate/postDetail/" + postId;
	}

	@GetMapping("/tag/{tagName}")
	public String getPostsByTag(@PathVariable("tagName") String tagName, HttpSession session, Model m) {
		List<Post> posts;
		List<Tag> tagNameList;
		try {
			// 태그 이름에서 #을 제거
			String cleanedTagName = tagName.replace("#", "");
			posts = dao.getPostsByTag(cleanedTagName);
			tagNameList = dao.getAllTags();
			m.addAttribute("postlist", posts);
			m.addAttribute("tagName", tagNameList);

			// 세션에서 userId 가져오기
			String userId = (String) session.getAttribute("userId");
			if (userId != null) {
				m.addAttribute("userId", userId); // userId가 있으면 모델에 추가
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("태그 기반 게시글 조회 과정에서 문제 발생!!");
			m.addAttribute("error", "태그 기반 게시글 조회가 정상적으로 처리되지 않았습니다!!");
		}
		return "MAIN"; // or "categoryView" depending on where you want to display the results
	}
}