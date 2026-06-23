package com.memorycard.repository;

import com.memorycard.entity.ArchiveFileType;
import com.memorycard.entity.GameArchiveFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameArchiveFileRepository extends JpaRepository<GameArchiveFile, Long> {

    List<GameArchiveFile> findByCartridgeIdOrderByUploadedAtDesc(Long cartridgeId);

    List<GameArchiveFile> findByGameIdOrderByUploadedAtDesc(Long gameId);

    Optional<GameArchiveFile> findByIdAndGameIdAndCartridgeId(Long id, Long gameId, Long cartridgeId);

    List<GameArchiveFile> findByGameIdAndFileTypeOrderByUploadedAtDesc(Long gameId, ArchiveFileType fileType);

    List<GameArchiveFile> findByCartridgeIdAndFileTypeOrderByUploadedAtDesc(Long cartridgeId, ArchiveFileType fileType);
}
