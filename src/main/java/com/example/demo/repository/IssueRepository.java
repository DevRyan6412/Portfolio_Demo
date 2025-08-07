package com.example.demo.repository;

import com.example.demo.domain.entity.Issue;
import com.example.demo.domain.entity.IssueStatus;
import com.example.demo.domain.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    // ✅ 특정 프로젝트에 속한 모든 이슈 조회 (페이징 지원)
    Page<Issue> findByProject(Project project, Pageable pageable);

    List<Issue> findByProject(Project project);

    // ✅ 특정 프로젝트 내 특정 이슈 조회
    Optional<Issue> findByIdAndProject(Long id, Project project);

    // ✅ 검색 기능 추가 (제목, 내용, 작성자에서 검색)
    @Query("SELECT i FROM Issue i WHERE i.project = :project AND " +
            "(LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.cName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Issue> searchIssues(@Param("project") Project project, @Param("keyword") String keyword, Pageable pageable);

    // ✅ 특정 프로젝트 내 특정 상태의 이슈 조회 (페이징 추가)
    Page<Issue> findByProjectAndStatus(Project project, IssueStatus status, Pageable pageable);

    // ✅ 특정 프로젝트 내 특정 상태 & 검색 키워드가 포함된 이슈 조회 (페이징 포함)
    @Query("SELECT i FROM Issue i WHERE i.project = :project AND i.status = :status AND " +
            "(LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.cName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Issue> searchIssuesByStatus(@Param("project") Project project,
                                     @Param("status") IssueStatus status,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);
}




//package com.example.demo.repository;
//
//import com.example.demo.domain.entity.Issue;
//import com.example.demo.domain.entity.IssueStatus;
//import com.example.demo.domain.entity.Project;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface IssueRepository extends JpaRepository<Issue, Long> {
//
//    // ✅ 특정 프로젝트에 속한 모든 이슈 조회
//    Page<Issue> findByProject(Project project, Pageable pageable);
//
//    List<Issue> findByProject(Project project);
//
//    // ✅ 특정 프로젝트 내 특정 이슈 조회
//    Optional<Issue> findByIdAndProject(Long id, Project project);
//
//    // ✅ 검색 기능 추가 (제목, 내용, 작성자에서 검색)
//    @Query("SELECT i FROM Issue i WHERE i.project = :project AND " +
//            "(LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
//            "OR LOWER(i.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
//            "OR LOWER(i.cName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
//    Page<Issue> searchIssues(@Param("project") Project project, @Param("keyword") String keyword, Pageable pageable);
//    // ✅ 추가: 특정 프로젝트 내에서 특정 상태의 이슈 조회
//    List<Issue> findByProjectAndStatus(Project project, IssueStatus status);
//}
//
