<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>게시글 목록</title>
</head>
<body>
    <h1>게시글 목록</h1>
    <ul>
        <c:forEach var="post" items="${posts}">
            <li>
                ${post.title}
            </li>
        </c:forEach>
    </ul>
</body>
</html>
