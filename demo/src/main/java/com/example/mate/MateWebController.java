package com.example.mate;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestPart;
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

    @GetMapping("/list")
	public String listPost(Model m) {
		List<Post> list;
		try {
			list = dao.getAll();
			m.addAttribute("postlist", list);
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("게시글 목록 생성 과정에서 문제 발생!!");
			m.addAttribute("error", "게시글 목록이 정상적으로 처리되지 않았습니다!!");
		}
		return "MAIN";
	}

	@PostMapping("/add")
	public String addPost(@ModelAttribute Post post, Model m, @RequestParam("file") MultipartFile file, @RequestParam("tagIds") List<Integer> tagIds) {
		try {
			// 저장 파일 객체 생성
			File dest = new File(fdir + "/" + file.getOriginalFilename());
			// 파일 저장
			file.transferTo(dest); // 업로드한 파일을 지정한 경로에 저장
			// 객체에 파일 이름 저장
			post.setImg("/img/" + dest.getName());
			dao.addPost(post, tagIds);
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("게시글 추가 과정에서 문제 발생!!");
			m.addAttribute("error", "게시글이 정상적으로 등록되지 않았습니다!!");
		}
		return "redirect:/MAIN";
	}

	@GetMapping("/delete/{aid}")
	public String deletePost(@PathVariable int aid, Model m) {
		try {
			dao.delPost(aid);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.warn("게시글 삭제 과정에서 문제 발생!!");
			m.addAttribute("error", "게시글이 정상적으로 삭제되지 않았습니다!!");
		}
		return "redirect:/MAIN";
	}

	@GetMapping("/{aid}")
	public String getPost(@PathVariable int aid, Model m) {
		try {
			Post n = dao.getPost(aid);
			m.addAttribute("news", n);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.warn("게시글을 가져오는 과정에서 문제 발생!!");
			m.addAttribute("error", "게시글을 정상적으로 가져오지 못했습니다!!");
		}
		return "postDetail";
	}
}