package jp.ymatsukawa.stockapi.domain.service.relation;

import jp.ymatsukawa.stockapi.domain.entity.bridge.BridgeInformationTags;
import jp.ymatsukawa.stockapi.domain.repository.InformationTagsRepository;
import jp.ymatsukawa.stockapi.domain.repository.TagRepository;
import jp.ymatsukawa.stockapi.tool.converter.ListConverter;

import java.util.*;

public class InformationTagsResource {
  private InformationTagsResource() {}
  private static class InformationTagsSupport {
    private static final InformationTagsResource INSTANCE = new InformationTagsResource();
  }

  public static InformationTagsResource getInstance() {
    return InformationTagsSupport.INSTANCE;
  }

  /**
   * get map of informationId to tag name list.
   * @param informationTagsRepository to get information and tag name
   * @param tags set of tag
   * @return Map&lt;Long, List&lt;String&gt;&gt; that is map of informationid to tag name list
   */
  public Map<Long, List<String>> getInformationidToTags(
    InformationTagsRepository informationTagsRepository,
    Set<String> tags
  ) {
    /**
     * get records of BridgeInformation(informationId, tag's name)
     *
     * ex.
     * [BridgeInformationTags(1, "example"),
     *  BridgeInformationTags(1, "sample"),
     *  BridgeInformationTags(2, "example"),
     *  ...]
     */
    List<BridgeInformationTags> informationTags = informationTagsRepository.findInformationByTag(tags, tags.size());
    return this.convertInformationIdToTags(informationTags);
  }

  public Map<Long, List<String>> getInformationidToTags(
    InformationTagsRepository informationTagsRepository,
    long informationId
  ) {
    /**
     * get records of BridgeInformation(informationId, tag's name)
     *
     * ex.
     * [BridgeInformationTags(1, "example"),
     *  BridgeInformationTags(1, "sample"),
     *  BridgeInformationTags(2, "example"),
     *  ...]
     */
    List<BridgeInformationTags> informationTags = informationTagsRepository.findTagByInformation(informationId);
    return this.convertInformationIdToTags(informationTags);
  }

  public void saveTagRelationNotYetStoraged(
    TagRepository tagRepository,
    String addedTags
  ) {
    /**
     * 1. save tag name which is not yet saved at DB.
     * 2. chains relation between informationId and tagId
     * ex. at 1.
     *
     * DB...
     * tag: name ... "foo", "qux", "sample", "example"
     * when parameter tags "foo,bar"
     * then
     * tag: name ... "foo", "qux", "sample", "example", "bar"
     */
    // 1. save tag name which is not yet saved at DB.
    Set<String> newAddedTags = new HashSet<>(ListConverter.getListBySplit(addedTags, ","));
    newAddedTags.removeAll(tagRepository.findSavedName(newAddedTags));
    if(!newAddedTags.isEmpty()) {
      tagRepository.save(newAddedTags);
    } else {
      return; // do nothing when added tags is does not exist
    }
  }

  public void chainsRelationBetweenInformationIdAndTag(
    InformationTagsRepository informationTagsRepository,
    long informationId, Set<String> addedTags
  ) {
    // chains relation between informationId and tagId
     informationTagsRepository.saveRelationByInfoIdAndTagNames(
      informationId,
      addedTags
    );
  }

  private Map<Long, List<String>> convertInformationIdToTags(List<BridgeInformationTags> informationTags) {
    /**
     * map BridgeInformationTags' informationId -> name
     *
     * ex.
     * {
     *   1 -> ["example", "sample"],
     *   2 -> ["example"]
     *   ...
     * }
     */
    Map<Long, List<String>> informationIdToTags = new HashMap<>();
    informationTags.forEach(informationTag -> {
      Long id = informationTag.getInformationId();
      String newTag = informationTag.getTag();

      if (informationIdToTags.get(id) == null) {
        informationIdToTags.put(id, new ArrayList<String>() {
          {
            add(newTag);
          }
        });
      } else {
        List<String> newTags = informationIdToTags.get(id);
        newTags.add(newTag);
        informationIdToTags.put(id, newTags);
      }
    });

    return informationIdToTags;
  }
}
