package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.davinkevin.podcastserver.business.ItemBusiness;
import com.github.davinkevin.podcastserver.business.WatchListBusiness;
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager;
import com.github.davinkevin.podcastserver.service.ByteRangeResourceHandler;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import com.github.davinkevin.podcastserver.entity.WatchList;
import org.slf4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.UUID;

/**
 * Created by kevin on 26/12/2013.
 */
@RestController
@RequestMapping("/api/podcasts/{idPodcast}/items")
public class ItemController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ItemController.class);
    private final ItemBusiness itemBusiness;
    private final ItemDownloadManager itemDownloadManager;
    private final ByteRangeResourceHandler handler;
    private final WatchListBusiness watchListBusiness;

    @java.beans.ConstructorProperties({"itemBusiness", "itemDownloadManager", "handler", "watchListBusiness"})
    public ItemController(ItemBusiness itemBusiness, ItemDownloadManager itemDownloadManager, ByteRangeResourceHandler handler, WatchListBusiness watchListBusiness) {
        this.itemBusiness = itemBusiness;
        this.itemDownloadManager = itemDownloadManager;
        this.handler = handler;
        this.watchListBusiness = watchListBusiness;
    }

    @GetMapping
    @JsonView(Item.ItemPodcastListView.class)
    public Page<Item> findAll(@PathVariable UUID idPodcast, Pageable pageable) {
        return itemBusiness.findByPodcast(idPodcast, pageable);
    }

    @GetMapping("{id}")
    @JsonView(Item.ItemDetailsView.class)
    public Item findById(@PathVariable UUID id) {
        return itemBusiness.findOne(id);
    }

    @PutMapping("{id}")
    @JsonView(Item.ItemDetailsView.class)
    public Item update(@RequestBody Item item, @PathVariable("id") UUID id) {
        item.setId(id);
        return itemBusiness.save(item);
    }

    @GetMapping("{id}/watchlists")
    @JsonView(Object.class)
    public Set<WatchList> getWatchListOfItem(@PathVariable("id") UUID id) {
        return watchListBusiness.findContainsItem(id);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(value = "id") UUID id) {
        itemBusiness.delete(id);
    }

    @GetMapping("{id}/addtoqueue")
    public void addToDownloadList(@PathVariable("id") UUID id) {
        itemDownloadManager.addItemToQueue(id);
    }

    @GetMapping("{id}/{file}")
    public void getEpisodeFile(@PathVariable UUID id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Item item = itemBusiness.findOne(id);
        if (item.isDownloaded()) {
            request.setAttribute(ByteRangeResourceHandler.Companion.getATTR_FILE(), item.getLocalPath());
            handler.handleRequest(request, response);
        } else {
            response.sendRedirect(item.getUrl());
        }
    }

    @GetMapping("{id}/cover{ext}")
    public ResponseEntity<?> getCover(@PathVariable UUID id) throws Exception {
        Item item = itemBusiness.findOne(id);
        Path cover = item.getCoverPath().getOrElseThrow(() -> new RuntimeException("File not found for item of id " + id));

        if (Files.notExists(cover))
            return ResponseEntity
                    .ok(new UrlResource(item.getCover().getUrl()));

        return ResponseEntity.ok()
                .lastModified(Files.getLastModifiedTime(cover).toMillis())
                .body(new FileSystemResource(cover.toFile()));
    }


    @GetMapping("{id}/reset")
    @JsonView(Item.ItemDetailsView.class)
    public Item reset(@PathVariable UUID id) {
        return itemBusiness.reset(id);
    }

    @PostMapping("/upload")
    @JsonView(Item.ItemDetailsView.class)
    public Item uploadFile(@PathVariable UUID idPodcast, @RequestPart("file") MultipartFile file) throws IOException, ParseException, URISyntaxException {
        return itemBusiness.addItemByUpload(idPodcast, file);
    }
}
