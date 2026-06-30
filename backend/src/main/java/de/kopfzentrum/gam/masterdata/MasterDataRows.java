package de.kopfzentrum.gam.masterdata;

import java.util.List;
import java.util.Map;

public record MasterDataRows(MasterDataCatalog catalog, List<Map<String, Object>> rows) {}
