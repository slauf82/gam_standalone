package de.kopfzentrum.gam.masterdata;

import java.util.List;

public record MasterDataCatalog(
  String key,
  String label,
  String module,
  String tableName,
  String primaryKey,
  List<String> fields,
  List<String> searchFields,
  String note
) {}
