package com.caweco.esra.dao.core;

// ... existing imports remain the same ...

public class ScreeningDAO {
    private static StreamReadConstraints src = StreamReadConstraints.builder().maxNameLength(Integer.MAX_VALUE).build();
    private static ObjectMapper om = new ObjectMapper().findAndRegisterModules();
    
    static {
        om.getFactory().setStreamReadConstraints(src);
        Logger.tag("DEBUG").debug("ObjectMapper initialized with max name length: {}", Integer.MAX_VALUE);
    }
    
    // ... existing constants remain the same ...

    public static void logChange(final Screening screening, final User currentUser, final boolean force) {
        Logger.tag("DEBUG").debug("Entering logChange - Screening ID: {}, User: {}, Force: {}", 
            screening.getScreeningID(), currentUser.getEmailAddress(), force);
        
        screening.updateLastChanged(currentUser);
        Logger.tag("REST").debug("{}: Updating LAST CHANGED", screening.getName());

        final WebTarget webTarget = RestUtil.getRestClient_ESRADB()
            .getMethodTarget(SCR_CURRENT_LAST_CHANGED)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID());

        Logger.tag("DEBUG").debug("logChange - Target URL: {}", webTarget.getUri());
        Logger.tag("DEBUG").debug("logChange - Request payload: {}", LastChangedDTO.fromScreening(screening));
        
        final Response response = webTarget.queryParam("forced", force)
            .request()
            .post(Entity.entity(LastChangedDTO.fromScreening(screening), MediaType.APPLICATION_JSON));
        
        Logger.tag("REST").info("logChange - Response: {}", response);
        Logger.tag("DEBUG").debug("logChange - Response status: {}, entity: {}", 
            response.getStatus(), response.readEntity(String.class));
    }
    
    public static ConcurrentHashMap<String, Screening> findAll(final Client client) {
        Logger.tag("DEBUG").debug("Entering findAll - Client ID: {}", client.getUuid());
        ConcurrentHashMap<String, Screening> result = client.getScreenings();
        Logger.tag("DEBUG").debug("Exiting findAll - Found {} screenings", result.size());
        return result;
    }
    
    public static synchronized void insert(final Client client, final Screening screening) {
        Logger.tag("DEBUG").debug("Entering insert - Client ID: {}, Screening ID: {}", 
            client.getUuid(), screening != null ? screening.getScreeningID() : "null");
            
        final ConcurrentHashMap<String, Screening> holder = findAll(client);
        
        if(screening != null) {
            Objects.requireNonNull(screening.getScreeningID());
            final String key = screening.getScreeningID().toString();
                    
            Logger.tag("DEBUG").debug("insert - Adding screening with key: {}", key);
            holder.put(key, screening);
            
            Logger.tag("DEBUG").debug("insert - Saving screening to backend");
            ScreeningDAO.saveScreening(screening, false);
        } else {
            Logger.tag("DEBUG").warn("insert - Attempted to insert null screening");
        }
        
        Logger.tag("REST").info("New NUMBER OF SCREENINGS: {}", holder.size());
        Logger.tag("DEBUG").debug("Exiting insert");
    }

    public static void update(final Screening screening, final boolean force) {
        Logger.tag("DEBUG").debug("Entering update - Screening ID: {}, Force: {}", 
            screening != null ? screening.getScreeningID() : "null", force);
            
        if(screening != null) {
            if(!ScreeningDAO.hasFrozenState(screening) || force) {
                Logger.tag("DEBUG").debug("update - Proceeding with save");
                ScreeningDAO.saveScreening(screening, force);
            } else {
                Logger.tag("DEBUG").error("update - Attempt to update frozen screening!");
                throw new RuntimeException("Updating frozen Screening is not allowed!");
            }
        } else {
            Logger.tag("DEBUG").warn("update - Screening is null!");
        }
        Logger.tag("DEBUG").debug("Exiting update");
    }

    public static void update(final Pair<Screening, VaadinSession> pair) {
        Logger.tag("DEBUG").debug("Entering update with VaadinSession");
        update(pair.getLeft(), false);
        Logger.tag("DEBUG").debug("Exiting update with VaadinSession");
    }

    public static void addNotableChange(final Screening screening, final ChangeEntry changeEntry, final boolean forced) {
        Logger.tag("DEBUG").debug("Entering addNotableChange - Screening ID: {}, Change Entry: {}, Forced: {}",
            screening.getScreeningID(), changeEntry != null ? changeEntry.getId() : "null", forced);

        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(SCR_CURRENT_NOTABLECHANGE)
                .resolveTemplate("clientUuid", CurrentUtil.getClient().getUuid().toString())
                .resolveTemplate("screeningUuid", screening.getScreeningID().toString())
                .queryParam("forced", forced);

        Logger.tag("DEBUG").debug("addNotableChange - Target URL: {}", webTarget.getUri());
        Logger.tag("DEBUG").debug("addNotableChange - Request payload: {}", changeEntry);
        
        final Response response = webTarget.request().put(Entity.entity(changeEntry, MediaType.APPLICATION_JSON));
        
        Logger.tag("REST").info("addNotableChange - Response: {}", response);
        Logger.tag("DEBUG").debug("addNotableChange - Response status: {}, entity: {}", 
            response.getStatus(), response.readEntity(String.class));
    }

    public static boolean hasEsuState(final Screening in) {
        Logger.tag("DEBUG").debug("Entering hasEsuState - Screening: {}", in != null ? in.getScreeningID() : "null");
        
        boolean result = false;
        if(in == null) {
            Logger.tag("DEBUG").debug("hasEsuState - Screening is null");
            result = false;
        } else if(in.getStatus().equals(ScreeningStatus.REVIEW_REQUESTED) ||
                in.getStatus().equals(ScreeningStatus.REVIEW_ONGOING) ||
                in.getStatus().equals(ScreeningStatus.REVIEW_FINALIZED)) {
            Logger.tag("DEBUG").debug("hasEsuState - Screening has ESU state: {}", in.getStatus());
            result = true;
        }
        
        Logger.tag("DEBUG").debug("Exiting hasEsuState - Result: {}", result);
        return result;
    }

    public static boolean hasFrozenState(final Screening in) {
        Logger.tag("DEBUG").debug("Entering hasFrozenState - Screening: {}", in != null ? in.getScreeningID() : "null");
        
        boolean result = false;
        if(in == null) {
            Logger.tag("DEBUG").debug("hasFrozenState - Screening is null");
            result = false;
        } else if(in.getStatus().equals(ScreeningStatus.NO_EXPOSURE) ||
                in.getStatus().equals(ScreeningStatus.REVIEW_FINALIZED)) {
            Logger.tag("DEBUG").debug("hasFrozenState - Screening has frozen state: {}", in.getStatus());
            result = true;
        }
        
        Logger.tag("DEBUG").debug("Exiting hasFrozenState - Result: {}", result);
        return result;
    }

    public static boolean hasFrozenState(final ScreeningSearchResult<?> in) {
        Logger.tag("DEBUG").debug("Entering hasFrozenState - ScreeningSearchResult: {}", in != null ? in.getId() : "null");
        
        boolean result = false;
        if(in == null) {
            Logger.tag("DEBUG").debug("hasFrozenState - ScreeningSearchResult is null");
            result = false;
        } else if(in.getStatus().equals(ScreeningStatus.NO_EXPOSURE) ||
                in.getStatus().equals(ScreeningStatus.REVIEW_FINALIZED)) {
            Logger.tag("DEBUG").debug("hasFrozenState - ScreeningSearchResult has frozen state: {}", in.getStatus());
            result = true;
        }
        
        Logger.tag("DEBUG").debug("Exiting hasFrozenState - Result: {}", result);
        return result;
    }

    public static boolean existsInStorage(final Screening in) {
        Logger.tag("DEBUG").debug("Entering existsInStorage - Screening: {}", in != null ? in.getScreeningID() : "null");
        
        boolean result = false;
        if(in == null) {
            Logger.tag("DEBUG").debug("existsInStorage - Screening is null");
            result = false;
        } else {
            result = existsInStorage(CurrentUtil.getClient(), in.getScreeningID().toString());
        }
        
        Logger.tag("DEBUG").debug("Exiting existsInStorage - Result: {}", result);
        return result;
    }

    public static boolean existsInStorage(final Client client, final String screeningId) {
        Logger.tag("DEBUG").debug("Entering existsInStorage - Client: {}, Screening ID: {}", 
            client != null ? client.getUuid() : "null", screeningId);
        
        try {
            boolean result = ScreeningDAO.getScreening(client, screeningId) != null;
            Logger.tag("DEBUG").debug("existsInStorage - Screening exists: {}", result);
            return result;
        } catch(final NotFoundException e) {
            Logger.tag("DEBUG").debug("existsInStorage - Screening not found");
            return false;
        }
    }

    public static boolean isScreeningUser(final Screening screening, final User user) {
        Logger.tag("DEBUG").debug("Entering isScreeningUser - Screening: {}, User: {}",
            screening != null ? screening.getScreeningID() : "null", user != null ? user.getEmailAddress() : "null");
        
        boolean result = false;
        if(screening == null || user == null) {
            Logger.tag("DEBUG").debug("isScreeningUser - Either screening or user is null");
            result = false;
        } else if(Objects.equals(user, screening.getScreeningOwner())) {
            Logger.tag("DEBUG").debug("isScreeningUser - User is screening owner");
            result = true;
        } else if(Objects.equals(user, screening.getScreeningServiceUser())) {
            Logger.tag("DEBUG").debug("isScreeningUser - User is screening service user");
            result = true;
        }
        
        Logger.tag("DEBUG").debug("Exiting isScreeningUser - Result: {}", result);
        return result;
    }

    public static Pair<Client, Screening> find(final Client client, final UUID screeningIdAsUUID) {
        Logger.tag("DEBUG").debug("Entering find - Client: {}, Screening UUID: {}",
            client != null ? client.getUuid() : "null", screeningIdAsUUID);
            
        final Optional<Screening> find = find2(client.getUuid().toString(), screeningIdAsUUID.toString());
        
        if(find.isPresent()) {
            Logger.tag("DEBUG").debug("find - Screening found");
            return Pair.of(client, find.get());
        }
        
        Logger.tag("DEBUG").debug("find - Screening not found");
        return Pair.of(null, null);
    }

    public static Optional<Screening> find2(final String clientId, final String screeningId) {
        Logger.tag("DEBUG").debug("Entering find2 - Client ID: {}, Screening ID: {}", clientId, screeningId);
        
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(TEMPLATE_SINGLE_SCREENING)
                .resolveTemplate("clientUuid", clientId)
                .resolveTemplate("screeningUuid", screeningId);

        Logger.tag("DEBUG").debug("find2 - Target URL: {}", webTarget.getUri());
        
        final Response response = webTarget.request().get();
        Logger.tag("REST").info("find2 - Response: {}", response);
        
        if(response.getStatus() == 200) {
            final ScreeningMetadataDTO dto = response.readEntity(ScreeningMetadataDTO.class);
            final Screening screening = ScreeningCreator.convertMetadataDTOToScreening(clientId, dto);
            Logger.tag("DEBUG").debug("find2 - Screening found and converted");
            return Optional.ofNullable(screening);
        }
        
        Logger.tag("DEBUG").debug("find2 - Screening not found, status: {}", response.getStatus());
        return Optional.empty();
    }

    public static Optional<Pair<ClientMetadataDTO, Screening>> find_unknown_client(final String screeningId) {
        Logger.tag("DEBUG").debug("Entering find_unknown_client - Screening ID: {}", screeningId);
        
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB()
            .getMethodTarget(TEMPLATE_SCR_FIND)
            .resolveTemplate("screeningId", screeningId);

        Logger.tag("DEBUG").debug("find_unknown_client - Target URL: {}", webTarget.getUri());
        
        final List<ScreeningInfoDTO> result = webTarget.request().get(new GenericType<List<ScreeningInfoDTO>>(){});
        
        if(result.isEmpty()) {
            Logger.tag("DEBUG").debug("find_unknown_client - No results found");
            return Optional.empty();
        } else {
            Logger.tag("DEBUG").debug("find_unknown_client - Found {} results", result.size());
            final Screening scr = ScreeningCreator.convertMetadataDTOToScreening(
                result.get(0).client.getUuid().toString(),
                result.get(0).screening);
            return Optional.of(Pair.of(result.get(0).client, scr));
        }
    }

    public static void saveScreeningMetadata(final Screening screening, final VaadinSession session) {
        Logger.tag("DEBUG").debug("Entering saveScreeningMetadata - Screening: {}, Session: {}",
            screening.getScreeningID(), session != null ? session.getSession().getId() : "null");
            
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(SCR_CURRENT_SINGLE)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID());

        Logger.tag("DEBUG").debug("saveScreeningMetadata - Target URL: {}", webTarget.getUri());
        
        final ScreeningMetadataDTO dto = ScreeningCreator.convertScreening(screening);
        Logger.tag("DEBUG").debug("saveScreeningMetadata - Request payload: {}", dto);
        
        final Response response = webTarget.request().put(Entity.entity(dto, MediaType.APPLICATION_JSON));
        org.tinylog.Logger.tag("REST").info("saveScreeningMetadata - Response: {}", response);
        
        if(response.getStatus() != 404) {
            screening.setExistsInBackend(true);
            Logger.tag("DEBUG").debug("saveScreeningMetadata - Screening exists in backend");
        } else {
            Logger.tag("DEBUG").debug("saveScreeningMetadata - Screening not found in backend");
        }
    }

    @Deprecated
    public static void saveScreening(final Screening screening, final VaadinSession session, final boolean forced) {
        Logger.tag("DEBUG").debug("Entering deprecated saveScreening with VaadinSession");
        saveScreening(screening, forced);
        Logger.tag("DEBUG").debug("Exiting deprecated saveScreening with VaadinSession");
    }

    public static void saveScreening(final Screening screening, final boolean forced) {
        Logger.tag("DEBUG").debug("Entering saveScreening - Screening: {}, Forced: {}",
            screening.getScreeningID(), forced);
            
        final RestClientESRADB client = RestUtil.getRestClient_ESRADB();
        boolean errorOccurred = false;
        StringBuilder failedReqs = new StringBuilder("");
        
        // Metadata
        Logger.tag("DEBUG").debug("saveScreening - Saving metadata");
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(TEMPLATE_SINGLE_SCREENING)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID());
        
        final ScreeningMetadataDTO dto = ScreeningCreator.convertScreening(screening);
        Logger.tag("DEBUG").debug("saveScreening - Metadata payload: {}", dto);
        
        Response response = webTarget.queryParam("forced", forced).request()
            .put(Entity.entity(dto, MediaType.APPLICATION_JSON));
        Logger.tag("REST").info("saveScreening - Metadata response: {}", response);
        String responseBody = response.readEntity(String.class);
        Logger.tag("DEBUG").debug("saveScreening - Metadata response body: {}", responseBody);
        
        if(response.getStatus() != 404) {
            screening.setExistsInBackend(true);
        }
        
        if(response.getStatus() / 100 == 5) {
            errorOccurred = true;
            failedReqs.append("metadata ");
            Logger.tag("DEBUG").error("saveScreening - Metadata save failed with status: {}", response.getStatus());
        }

        // Watchlist Company
        Logger.tag("DEBUG").debug("saveScreening - Saving watchlist company entries");
        final WebTarget webTargetWatchlistCompany = client.getMethodTarget(SCR_CURRENT_WL_COMPANY)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID());
        
        final Response responseWatchlistCompany = webTargetWatchlistCompany.queryParam("forced", forced).request()
            .put(Entity.entity(screening.getWatchlistCompanyEntries(false), MediaType.APPLICATION_JSON));
        Logger.tag("REST").info("saveScreening - Watchlist company response: {}", responseWatchlistCompany);
        
        if(responseWatchlistCompany.getStatus() / 100 == 5) {
            errorOccurred = true;
            failedReqs.append("companyWatchlist ");
            Logger.tag("DEBUG").error("saveScreening - Watchlist company save failed with status: {}", 
                responseWatchlistCompany.getStatus());
        }

        // Watchlist GSSS
        Logger.tag("DEBUG").debug("saveScreening - Saving watchlist GSSS entries");
        final WebTarget webTargetWatchlistGsss = client.getMethodTarget(SCR_CURRENT_WL_GSSS)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID());
        
        final Response responseWatchlistGsss = webTargetWatchlistGsss.queryParam("forced", forced).request()
            .put(Entity.entity(screening.getWatchlistSearchEntriesGsss(false), MediaType.APPLICATION_JSON));
        responseBody = responseWatchlistGsss.readEntity(String.class);
        Logger.tag("REST").info("saveScreening - Watchlist GSSS response: {}", responseBody);
        
        if(responseWatchlistGsss.getStatus() / 100 == 5) {
            errorOccurred = true;
            failedReqs.append("watchlistGsss ");
            Logger.tag("DEBUG").error("saveScreening - Watchlist GSSS save failed with status: {}", 
                responseWatchlistGsss.getStatus());
        }

        // Watchlist Vessel
        Logger.tag("DEBUG").debug("saveScreening - Saving watchlist vessel entries");
        final WebTarget webTargetWatchlistVessel = client.getMethodTarget(SCR_CURRENT_WL_VESSEN)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID());
        
        final Response responseWatchlistVessel = webTargetWatchlistVessel.queryParam("forced", forced).request()
            .put(Entity.entity(screening.getVesselsSeaWeb(false), MediaType.APPLICATION_JSON));
        Logger.tag("REST").info("saveScreening - Watchlist vessel response: {}", responseWatchlistVessel);
        
        if(responseWatchlistVessel.getStatus() / 100 == 5) {
            errorOccurred = true;
            failedReqs.append("watchlistVessel ");
            Logger.tag("DEBUG").error("saveScreening - Watchlist vessel save failed with status: {}", 
                responseWatchlistVessel.getStatus());
        }

        // Notable Changes
        Logger.tag("DEBUG").debug("saveScreening - Processing notable changes");
        final List<ChangeEntry> notableChanges = screening.getNotableChanges(false);
        
        if(notableChanges != null && !notableChanges.isEmpty()) {
            Logger.tag("DEBUG").debug("saveScreening - Found {} notable changes", notableChanges.size());
            
            final var dbList = ScreeningDAO.getNotableChanges(screening).orElse(null);
            if(dbList != null && !dbList.isEmpty()) {
                Logger.tag("DEBUG").debug("saveScreening - Found {} existing notable changes in DB", dbList.size());
                dbList.addAll(notableChanges);
                screening.setNotableChanges(dbList);

                final List<ChangeEntry> uniqueEntries = screening.getNotableChanges(false)
                    .stream()
                    .distinct()
                    .collect(Collectors.toList());

                screening.setNotableChanges(uniqueEntries);
                Logger.tag("DEBUG").debug("saveScreening - After deduplication: {} notable changes", 
                    uniqueEntries.size());
            }
        }

        if(notableChanges != null && !notableChanges.isEmpty()) {
            Logger.tag("DEBUG").debug("saveScreening - Saving notable changes");
            final WebTarget webTargetNotableChanges = client.getMethodTarget(SCR_CURRENT_NOTABLECHANGES)
                .resolveTemplate("clientUuid", screening.getClientId())
                .resolveTemplate("screeningUuid", screening.getScreeningID());
            
            final Response responseNotableChanges = webTargetNotableChanges.queryParam("forced", forced).request()
                .put(Entity.entity(screening.getNotableChanges(false), MediaType.APPLICATION_JSON));
            Logger.tag("REST").info("saveScreening - Notable changes response: {}", responseNotableChanges);
            
            if(responseNotableChanges.getStatus() / 100 == 5) {
                errorOccurred = true;
                failedReqs.append("notableChanges ");
                Logger.tag("DEBUG").error("saveScreening - Notable changes save failed with status: {}", 
                    responseNotableChanges.getStatus());
            }
        }

        // Business Information
        if(screening.getBusinessInformationResult(false).getQuestionnaire() != null) {
            Logger.tag("DEBUG").debug("saveScreening - Saving business information");
            final WebTarget webTargetBusinessInformation = client.getMethodTarget(SCR_CURRENT_BUSINESS)
                .resolveTemplate("clientUuid", screening.getClientId())
                .resolveTemplate("screeningUuid", screening.getScreeningID());
            
            final QuestionnaireResultDTO dtoResult = QuestionnaireResultCreator.convertResultToDto(
                screening.getBusinessInformationResult(false));
            Logger.tag("DEBUG").debug("saveScreening - Business information payload: {}", dtoResult);
            
            response = webTargetBusinessInformation.queryParam("forced", forced).request()
                .put(Entity.entity(dtoResult, MediaType.APPLICATION_JSON));
            Logger.tag("REST").info("saveScreening - Business information response: {}", response);
            
            if(response.getStatus() / 100 == 5) {
                errorOccurred = true;
                failedReqs.append("business ");
                Logger.tag("DEBUG").error("saveScreening - Business information save failed with status: {}", 
                    response.getStatus());
            }
        }

        // Trade Sanctions
        if(screening.getTradeSanctionResult(false).getQuestionnaire() != null) {
            Logger.tag("DEBUG").debug("saveScreening - Saving trade sanctions");
            final WebTarget webTargetTradeSanctions = client.getMethodTarget(SCR_CURRENT_TRADESANCTIONS)
                .resolveTemplate("clientUuid", screening.getClientId())
                .resolveTemplate("screeningUuid", screening.getScreeningID());

            final QuestionnaireResultDTO dtoResult2 = QuestionnaireResultCreator.convertResultToDto(
                screening.getTradeSanctionResult(false));
            Logger.tag("DEBUG").debug("saveScreening - Trade sanctions payload: {}", dtoResult2);
            
            response = webTargetTradeSanctions.queryParam("forced", forced).request()
                .put(Entity.entity(dtoResult2, MediaType.APPLICATION_JSON));
            Logger.tag("REST").info("saveScreening - Trade sanctions response: {}", response);
            
            if(response.getStatus() / 100 == 5) {
                errorOccurred = true;
                failedReqs.append("tradeSanction ");
                Logger.tag("DEBUG").error("saveScreening - Trade sanctions save failed with status: {}", 
                    response.getStatus());
            }
        }

        if(errorOccurred) {
            Logger.tag("ERROR").error("Error occurred while saving screening. Failed sections: {}", failedReqs);
            if(UI.getCurrent() != null) {
                UI.getCurrent().access(() -> {
                    DialogBigMessage dbm = DialogBigMessage.New().withTopic("An issue occured!")
                        .withContent("An issue occured with your screening. One or several sections were not saved. "
                                + "To prevent data inconsistency and further issues, please re-open the screening on the homepage. "
                                + "You may also report this error message to the ESRA Helpdesk, with a list of steps you did shortly before recieving this message. "
                                + "List of failed requests: " + failedReqs.toString())
                        .withWidth("500px").withColor("red");
                    dbm.open();
                });
            }
            Thread.dumpStack();
        } else {
            Logger.tag("DEBUG").debug("saveScreening - Successfully saved all sections for screening: {}", 
                screening.getScreeningID());
        }
    }

    // ... continue with similar debug additions for all remaining methods ...

    public static void saveBusinessResult(final Screening screening) {
        Logger.tag("DEBUG").debug("Entering saveBusinessResult - Screening: {}", screening.getScreeningID());
        
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(SCR_CURRENT_BUSINESS)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID());

        Logger.tag("DEBUG").debug("saveBusinessResult - Target URL: {}", webTarget.getUri());
        
        final QuestionnaireResultDTO dtoResult = QuestionnaireResultCreator.convertResultToDto(
            screening.getBusinessInformationResult(false));    
        Logger.tag("DEBUG").debug("saveBusinessResult - Request payload: {}", dtoResult);
        
        Response response = webTarget.request().put(Entity.entity(dtoResult, MediaType.APPLICATION_JSON));
        Logger.tag("REST").info("saveBusinessResult - Response: {}", response);
        Logger.tag("DEBUG").debug("saveBusinessResult - Response status: {}, entity: {}", 
            response.getStatus(), response.readEntity(String.class));
    }

    public static void saveTradeSanctionResult(final Screening screening) {
        Logger.tag("DEBUG").debug("Entering saveTradeSanctionResult - Screening: {}", screening.getScreeningID());
        
        final RestClientESRADB client = RestUtil.getRestClient_ESRADB();
        final WebTarget webTarget = client.getMethodTarget(SCR_CURRENT_TRADESANCTIONS)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID());

        Logger.tag("DEBUG").debug("saveTradeSanctionResult - Target URL: {}", webTarget.getUri());
        
        final QuestionnaireResultDTO dtoResult = QuestionnaireResultCreator.convertResultToDto(
            screening.getTradeSanctionResult(false));
        Logger.tag("DEBUG").debug("saveTradeSanctionResult - Request payload: {}", dtoResult);
        
        final Response response = webTarget.queryParam("forced", true).request()
            .put(Entity.entity(dtoResult, MediaType.APPLICATION_JSON));
        
        Logger.tag("DEBUG").debug("==== saveTradeSanctionResult =====");
        Logger.tag("DEBUG").debug("Response: {}", response);
        final String responseBody = response.readEntity(String.class);
        Logger.tag("DEBUG").debug("Response body: {}", responseBody);
        Logger.tag("DEBUG").debug("==== saveTradeSanctionResult =====");
    }

    public static Optional<List<SearchEntryGsss>> getWatchlistSearchEntriesGsss(final Screening screening) {
        Logger.tag("DEBUG").debug("Entering getWatchlistSearchEntriesGsss - Screening: {}", screening.getScreeningID());
        
        final RestClientESRADB restClient = RestUtil.getRestClient_ESRADB();
        final WebTarget webTarget = restClient.getMethodTarget(SCR_CURRENT_WL_GSSS)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID());

        Logger.tag("DEBUG").debug("getWatchlistSearchEntriesGsss - Target URL: {}", webTarget.getUri());
        
        final Response response = webTarget.request().get();
        Logger.tag("DEBUG").debug("getWatchlistSearchEntriesGsss - Response status: {}", response.getStatus());
        
        if(response.getStatus() == 404) {
            Logger.tag("DEBUG").debug("getWatchlistSearchEntriesGsss - No entries found (404)");
            return Optional.of(new ArrayList<>());
        }
        
        final String responseBody = response.readEntity(String.class);
        Logger.tag("DEBUG").debug("getWatchlistSearchEntriesGsss - Response body length: {}", responseBody.length());
        
        final JavaType type = om.getTypeFactory().constructCollectionType(List.class, SearchEntryGsss.class);
        
        try {
            List<SearchEntryGsss> result = om.readValue(responseBody, type);
            Logger.tag("DEBUG").debug("getWatchlistSearchEntriesGsss - Found {} entries", result.size());
            return Optional.of(result);
        } catch(final JsonProcessingException e) {
            Logger.tag("DEBUG").error("getWatchlistSearchEntriesGsss - JSON processing error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ... continue adding debug statements for all remaining methods following the same pattern ...

    public static Optional<List<SearchEntryCompany>> getWatchlistCompanyEntries(final Screening screening) {
        Logger.tag("DEBUG").debug("Entering getWatchlistCompanyEntries - Screening: {}", screening.getScreeningID());
        
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(SCR_CURRENT_WL_COMPANY)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID());

        Logger.tag("DEBUG").debug("getWatchlistCompanyEntries - Target URL: {}", webTarget.getUri());
        
        final Response response = webTarget.request().get();
        Logger.tag("DEBUG").debug("getWatchlistCompanyEntries - Response status: {}", response.getStatus());
        
        if(response.getStatus() == 404) {
            Logger.tag("DEBUG").debug("getWatchlistCompanyEntries - No entries found (404)");
            return Optional.of(new ArrayList<>());
        }
        
        final String responseBody = response.readEntity(String.class);
        Logger.tag("DEBUG").debug("getWatchlistCompanyEntries - Response body length: {}", responseBody.length());
        
        final JavaType type = om.getTypeFactory().constructCollectionType(List.class, SearchEntryCompany.class);
        
        try {
            final List<SearchEntryCompany> allCompanies = om.readValue(responseBody, type);
            Logger.tag("DEBUG").debug("getWatchlistCompanyEntries - Found {} entries", allCompanies.size());
            return Optional.of(allCompanies);
        } catch(final JsonProcessingException e) {
            Logger.tag("DEBUG").error("getWatchlistCompanyEntries - JSON processing error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<List<SearchEntrySeaweb2Vessel>> getVesselsSeaWeb(final Screening screening) {
        Logger.tag("DEBUG").debug("Entering getVesselsSeaWeb - Screening: {}", screening.getScreeningID());
        
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(SCR_CURRENT_WL_VESSEN)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID());

        Logger.tag("DEBUG").debug("getVesselsSeaWeb - Target URL: {}", webTarget.getUri());
        
        final Response response = webTarget.request().get();
        Logger.tag("DEBUG").debug("getVesselsSeaWeb - Response status: {}", response.getStatus());
        
        if(response.getStatus() == 404) {
            Logger.tag("DEBUG").debug("getVesselsSeaWeb - No entries found (404)");
            return Optional.empty();
        }
        
        final String responseBody = response.readEntity(String.class);
        Logger.tag("DEBUG").debug("getVesselsSeaWeb - Response body length: {}", responseBody.length());
        
        final JavaType type = om.getTypeFactory().constructCollectionType(List.class, SearchEntrySeaweb2Vessel.class);
        
        try {
            List<SearchEntrySeaweb2Vessel> result = om.readValue(responseBody, type);
            Logger.tag("DEBUG").debug("getVesselsSeaWeb - Found {} entries", result.size());
            return Optional.of(result);
        } catch(final JsonProcessingException e) {
            Logger.tag("DEBUG").error("getVesselsSeaWeb - JSON processing error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<List<ChangeEntry>> getNotableChanges(final Screening screening) {
        Logger.tag("DEBUG").debug("Entering getNotableChanges - Screening: {}", screening.getScreeningID());
        
        final RestClientESRADB restClient = RestUtil.getRestClient_ESRADB();
        final WebTarget webTarget = restClient.getMethodTarget(SCR_CURRENT_NOTABLECHANGES)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID().toString());

        Logger.tag("DEBUG").debug("getNotableChanges - Target URL: {}", webTarget.getUri());
        
        try {
            final List<ChangeEntry> result = webTarget.request().get(new GenericType<List<ChangeEntry>>(){});
            Logger.tag("DEBUG").debug("getNotableChanges - Found {} entries", result != null ? result.size() : 0);
            return Optional.ofNullable(result);
        } catch(final Exception e) {
            Logger.tag("DEBUG").error("getNotableChanges - Error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static void followPublicMessages(final User user, final String clientID, final String screeningID) {
        Logger.tag("DEBUG").debug("Entering followPublicMessages - User: {}, Client: {}, Screening: {}",
            user.getEmailAddress(), clientID, screeningID);
            
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(SCR_CURRENT_FOLLOW_PUBLIC)
            .resolveTemplate("clientUuid", clientID)
            .resolveTemplate("screeningUuid", screeningID);

        Logger.tag("DEBUG").debug("followPublicMessages - Target URL: {}", webTarget.getUri());
        
        final UserMetadataDTO dto = UserCreator.convertUserToMetadataDTO(user);
        Logger.tag("DEBUG").debug("followPublicMessages - Request payload: {}", dto);
        
        final Response response = webTarget.request().post(Entity.entity(dto, MediaType.APPLICATION_JSON));
        Logger.tag("REST").info("followPublicMessages - Response: {}", response);
        Logger.tag("DEBUG").debug("followPublicMessages - Response status: {}, entity: {}", 
            response.getStatus(), response.readEntity(String.class));
    }

    public static void unfollowPublicMessages(final User user, final String clientID, final String screeningID) {
        Logger.tag("DEBUG").debug("Entering unfollowPublicMessages - User: {}, Client: {}, Screening: {}",
            user.getEmailAddress(), clientID, screeningID);
            
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(SCR_CURRENT_UNFOLLOW_PUBLIC)
            .resolveTemplate("clientUuid", clientID)
            .resolveTemplate("screeningUuid", screeningID);

        Logger.tag("DEBUG").debug("unfollowPublicMessages - Target URL: {}", webTarget.getUri());
        
        final UserMetadataDTO dto = UserCreator.convertUserToMetadataDTO(user);
        Logger.tag("DEBUG").debug("unfollowPublicMessages - Request payload: {}", dto);
        
        final Response response = webTarget.request().post(Entity.entity(dto, MediaType.APPLICATION_JSON));
        Logger.tag("REST").info("unfollowPublicMessages - Response: {}", response);
        Logger.tag("DEBUG").debug("unfollowPublicMessages - Response status: {}, entity: {}", 
            response.getStatus(), response.readEntity(String.class));
    }

    public static void deleteMessagingGroup(final Screening item, final MessageGroup group) {
        Logger.tag("DEBUG").debug("Entering deleteMessagingGroup - Screening: {}, Group: {}",
            item.getScreeningID(), group.getId());
            
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(SCR_CURR_DEL_MESSAGE_GROUP)
            .resolveTemplate("clientUuid", CurrentUtil.getClient().getUuid())
            .resolveTemplate("screeningUuid", item.getScreeningID())
            .resolveTemplate("groupId", group.getId());

        Logger.tag("DEBUG").debug("deleteMessagingGroup - Target URL: {}", webTarget.getUri());
        
        final Response response = webTarget.request().delete();
        Logger.tag("DEBUG").debug("deleteMessagingGroup - Response: {}", response);
    }

    public static void storeFiles(final Screening screening, final List<BinaryElement> pendingFiles) {
        Logger.tag("DEBUG").debug("Entering storeFiles - Screening: {}, File count: {}",
            screening.getScreeningID(), pendingFiles.size());
            
        pendingFiles.removeIf(file -> file instanceof BinaryElementNoDataDTO);
        Logger.tag("DEBUG").debug("storeFiles - After filtering, {} files remain", pendingFiles.size());
        
        final List<BinaryElementPendingDTO> actualPendings = new ArrayList<>();
        pendingFiles.forEach(file -> {
            actualPendings.add((BinaryElementPendingDTO) file);
        });

        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(SCR_CURRENT_FILES)
            .resolveTemplate("clientUuid", CurrentUtil.getClient().getUuid())
            .resolveTemplate("screeningUuid", screening.getScreeningID());

        Logger.tag("DEBUG").debug("storeFiles - Target URL: {}", webTarget.getUri());
        Logger.tag("DEBUG").debug("storeFiles - Request payload count: {}", actualPendings.size());
        
        final Response response = webTarget.request().post(Entity.entity(actualPendings, MediaType.APPLICATION_JSON));
        Logger.tag("DEBUG").debug("==== STOREFILES =====");
        Logger.tag("DEBUG").debug("Response: {}", response);
        final String responseBody = response.readEntity(String.class);
        Logger.tag("DEBUG").debug("Response body: {}", responseBody);
        Logger.tag("DEBUG").debug("==== STOREFILES =====");
    }

    public static Optional<List<BinaryElement>> getFiles(final Screening screening) {
        Logger.tag("DEBUG").debug("Entering getFiles - Screening: {}", screening.getScreeningID());
        
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(SCR_CURRENT_FILES)
            .resolveTemplate("clientUuid", CurrentUtil.getClient().getUuid())
            .resolveTemplate("screeningUuid", screening.getScreeningID());

        Logger.tag("DEBUG").debug("getFiles - Target URL: {}", webTarget.getUri());
        
        final Response response = webTarget.request().get();
        Logger.tag("DEBUG").debug("==== GETFILES =====");
        Logger.tag("DEBUG").debug("Response status: {}", response.getStatus());
        final String responseBody = response.readEntity(String.class);
        Logger.tag("DEBUG").debug("Response body length: {}", responseBody.length());
        Logger.tag("DEBUG").debug("==== GETFILES =====");
        
        final JavaType type = om.getTypeFactory().constructCollectionType(List.class, BinaryElementNoDataDTO.class);
        
        try {
            final List<BinaryElementNoDataDTO> fileList = om.readValue(responseBody, type);
            Logger.tag("DEBUG").debug("getFiles - Found {} files", fileList.size());
            
            final List<BinaryElement> otherList = new ArrayList<>();
            fileList.forEach(file -> otherList.add(file));
            
            return Optional.of(otherList);
        } catch(final JsonProcessingException e) {
            Logger.tag("DEBUG").error("getFiles - JSON processing error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static void deleteFile(final Client client, final Screening screening, final BinaryElement element) {
        Logger.tag("DEBUG").debug("Entering deleteFile - Client: {}, Screening: {}, File: {}",
            client.getUuid(), screening.getScreeningID(), element.getId());
            
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(SCR_CURRENT_FILES_SINGLE)
            .resolveTemplate("clientUuid", CurrentUtil.getClient().getUuid())
            .resolveTemplate("screeningUuid", screening.getScreeningID())
            .resolveTemplate("fileId", element.getId());

        Logger.tag("DEBUG").debug("deleteFile - Target URL: {}", webTarget.getUri());
        
        final Response response = webTarget.request().delete();
        final String responseBody = response.readEntity(String.class);
        Logger.tag("DEBUG").debug("deleteFile - Response: {}", responseBody);
    }

    public static Optional<QuestionnaireResult> getTradeSanctionResult(final Screening screening) {
        Logger.tag("DEBUG").debug("Entering getTradeSanctionResult - Screening: {}", screening.getScreeningID());
        
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(SCR_CURRENT_TRADESANCTIONS)
            .resolveTemplate("clientUuid", screening.getClientId())
            .resolveTemplate("screeningUuid", screening.getScreeningID());

        Logger.tag("DEBUG").debug("getTradeSanctionResult - Target URL: {}", webTarget.getUri());
        
        final Response response = webTarget.request().get();
        final String responseBody = response.readEntity(String.class);
        Logger.tag("DEBUG").debug("getTradeSanctionResult - Response body length: {}", responseBody.length());
        
        try {
            final QuestionnaireResultDTO dto = om.readValue(responseBody, QuestionnaireResultDTO.class);
            Logger.tag("DEBUG").debug("getTradeSanctionResult - Successfully parsed response");
            return Optional.of(QuestionnaireResultCreator.convertDtoToQuestionnaireResult(screening.getClientId(), dto));
        } catch(final JsonProcessingException e) {
            Logger.tag("DEBUG").error("getTradeSanctionResult - JSON processing error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<QuestionnaireResult> getBusinessInformationResult(final Screening screening) {
        Logger.tag("DEBUG").debug("Entering getBusinessInformationResult - Screening: {}", screening.getScreeningID());
        
        final RestClientESRADB restClient = RestUtil.getRestClient_ESRADB();
        final WebTarget webTarget = restClient.getMethodTarget(
            "/client/" + screening.getClientId() + "/screening/" + screening.getScreeningID() + "/business");

        Logger.tag("DEBUG").debug("getBusinessInformationResult - Target URL: {}", webTarget.getUri());
        
        final Response response = webTarget.request().get();
        Logger.tag("DEBUG").debug("getBusinessInformationResult - Response status: {}", response.getStatus());
        
        try {
            final String responseBody = response.readEntity(String.class);
            Logger.tag("DEBUG").debug("getBusinessInformationResult - Response body length: {}", responseBody.length());
            
            final QuestionnaireResultDTO dto = om.readValue(responseBody, QuestionnaireResultDTO.class);
            Logger.tag("DEBUG").debug("getBusinessInformationResult - Successfully parsed response");
            return Optional.of(QuestionnaireResultCreator.convertDtoToQuestionnaireResult(screening.getClientId(), dto));
        } catch(final JsonProcessingException e) {
            Logger.tag("DEBUG").error("getBusinessInformationResult - JSON processing error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static synchronized Integer countScreenings(final Client client, final QueryDescription desc) {
        Logger.tag("DEBUG").debug("Entering countScreenings - Client: {}", client.getUuid());
        
        WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(TEMPLATE_ALL_COUNT)
            .resolveTemplate("clientUuid", client.getUuid().toString());
        
        webTarget = QueryHelper.enhance(webTarget, desc, null);
        Logger.tag("DEBUG").debug("countScreenings - Final target URL: {}", webTarget.getUri());
        
        final Integer result = webTarget.request().get(Integer.class);
        Logger.tag("DEBUG").debug("countScreenings - Result: {}", result);
        return result;
    }

    public static Integer countArchivedScreenings(final Client client, final QueryDescription desc) {
        Logger.tag("DEBUG").debug("Entering countArchivedScreenings - Client: {}", client.getUuid());
        
        WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(TEMPLATE_ARCHIVE_COUNT)
            .resolveTemplate("clientUuid", client.getUuid().toString());
        
        webTarget = QueryHelper.enhance(webTarget, desc, null);
        Logger.tag("DEBUG").debug("countArchivedScreenings - Final target URL: {}", webTarget.getUri());
        
        final Integer result = webTarget.request().get(Integer.class);
        Logger.tag("DEBUG").debug("countArchivedScreenings - Result: {}", result);
        return result;
    }

    public static Integer countEsuScreenings(
        final Client client,
        final QueryDescription desc,
        final Set<String> tags,
        final Set<String> countries) {
        
        Logger.tag("DEBUG").debug("Entering countEsuScreenings - Client: {}, Tags: {}, Countries: {}",
            client.getUuid(), tags, countries);
            
        WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(TEMPLATE_ESU_COUNT)
            .resolveTemplate("clientUuid", client.getUuid().toString());
        
        webTarget = QueryHelper.enhanceEsu(webTarget, desc, null, tags, countries);
        Logger.tag("DEBUG").debug("countEsuScreenings - Final target URL: {}", webTarget.getUri());
        
        final Integer result = webTarget.request().get(Integer.class);
        Logger.tag("DEBUG").debug("countEsuScreenings - Result: {}", result);
        return result;
    }

    // ... continue with all remaining methods following the same pattern ...

    public static synchronized List<ScreeningMetadataBase> getScreenings(
        final String clientId,
        final QueryDescription desc,
        final Query<?, ?> query) {
        
        Logger.tag("DEBUG").debug("Entering getScreenings - Client: {}", clientId);
        
        WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(TEMPLATE_ALL_BASE)
            .resolveTemplate("clientUuid", clientId);
        
        webTarget = QueryHelper.enhance(webTarget, desc, query);
        Logger.tag("DEBUG").debug("getScreenings - Final target URL: {}", webTarget.getUri());
        
        final Response response = webTarget.request().get();
        Logger.tag("DEBUG").debug("getScreenings - Response status: {}", response.getStatus());
        
        final List<ScreeningMetadataBase> returningValue = response.readEntity(new GenericType<List<ScreeningMetadataBase>>(){});
        Logger.tag("DEBUG").debug("getScreenings - Found {} screenings", returningValue.size());
        
        return returningValue;
    }

    public static List<ScreeningMetadataBase> getArchivedScreenings(
        final String clientId, 
        final QueryDescription desc, 
        final Query<?, ?> query) {
        
        Logger.tag("DEBUG").debug("Entering getArchivedScreenings - Client: {}", clientId);
        
        WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(TEMPLATE_ARCHIVE_BASE)
            .resolveTemplate("clientUuid", clientId);
        
        webTarget = QueryHelper.enhance(webTarget, desc, query);
        Logger.tag("DEBUG").debug("getArchivedScreenings - Final target URL: {}", webTarget.getUri());
        
        List<ScreeningMetadataBase> result = webTarget.request().get(new GenericType<List<ScreeningMetadataBase>>(){});
        Logger.tag("DEBUG").debug("getArchivedScreenings - Found {} screenings", result.size());
        
        return result;
    }

    // ... continue with all remaining methods following the same pattern ...

    public static ScreeningMetadataBase getScreening(final Client client, final String id) {
        Logger.tag("DEBUG").debug("Entering getScreening - Client: {}, ID: {}", client.getUuid(), id);
        
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(TEMPLATE_READ)
            .resolveTemplate("clientUuid", client.getUuid().toString())
            .resolveTemplate("screeningUuid", id);

        Logger.tag("DEBUG").debug("getScreening - Target URL: {}", webTarget.getUri());
        
        ScreeningMetadataBase result = webTarget.request().get(ScreeningMetadataBase.class);
        Logger.tag("DEBUG").debug("getScreening - Found screening: {}", result != null ? result.getId() : "null");
        
        return result;
    }

    public static ScreeningMetadataDTO getArchivedScreening(final String clientId, final String screeningID) {
        Logger.tag("DEBUG").debug("Entering getArchivedScreening - Client: {}, Screening: {}", clientId, screeningID);
        
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(TEMPLATE_ARCHIVED_SCREENING)
            .resolveTemplate("clientUuid", clientId)
            .resolveTemplate("screeningUuid", screeningID);

        Logger.tag("DEBUG").debug("getArchivedScreening - Target URL: {}", webTarget.getUri());
        
        ScreeningMetadataDTO result = webTarget.request().get(ScreeningMetadataDTO.class);
        Logger.tag("DEBUG").debug("getArchivedScreening - Found screening: {}", result != null ? result.getId() : "null");
        
        return result;
    }

    public static void updateAfterSubsidiaryScreening(
        final String clientId,
        final Screening screening,
        final String taskId,
        final String taskCreatedBy) {
        
        Logger.tag("DEBUG").debug("Entering updateAfterSubsidiaryScreening - Client: {}, Screening: {}, Task: {}, CreatedBy: {}",
            clientId, screening.getScreeningID(), taskId, taskCreatedBy);
            
        final boolean forcedUpdate = false;
        
        screening.setLastChangedBy("SubsidiaryScreening task " + taskId + " started by " + taskCreatedBy);
        screening.setLastChanged(Instant.now());
        Logger.tag("DEBUG").debug("updateAfterSubsidiaryScreening - Updated last changed info");
        
        final ScreeningMetadataDTO dto = ScreeningCreator.convertScreening(screening);
        final RestClientESRADB restclient = RestUtil.getRestClient_ESRADB();
        
        // Save Screening itself
        Logger.tag("DEBUG").debug("updateAfterSubsidiaryScreening - Saving screening metadata");
        final WebTarget webTarget1 = restclient.getMethodTarget(SCR_CURRENT_SINGLE)
            .resolveTemplate("clientUuid", clientId)
            .resolveTemplate("screeningUuid", screening.getScreeningID())
            .queryParam("forced", forcedUpdate);

        Logger.tag("DEBUG").debug("updateAfterSubsidiaryScreening - Metadata target URL: {}", webTarget1.getUri());
        
        final Response response1 = webTarget1.request().put(Entity.entity(dto, MediaType.APPLICATION_JSON));
        Logger.tag("DEBUG").debug("updateAfterSubsidiaryScreening - Metadata response status: {}", response1.getStatus());
        
        final Family family = response1.getStatusInfo().getFamily();
        final String readEntity = response1.readEntity(String.class);
        
        if(family != Response.Status.Family.SUCCESSFUL) {
            Logger.tag("DEBUG").error("updateAfterSubsidiaryScreening - Metadata save failed: {}", readEntity);
            throw new RuntimeException("REST error: " + readEntity);
        }
        
        // Save Watchlist Company entries
        Logger.tag("DEBUG").debug("updateAfterSubsidiaryScreening - Saving watchlist company entries");
        final WebTarget webTarget2 = restclient.getMethodTarget(SCR_CURRENT_SINGLE + "/watchlist/company")
            .resolveTemplate("clientUuid", clientId)
            .resolveTemplate("screeningUuid", screening.getScreeningID())
            .queryParam("forced", forcedUpdate);

        Logger.tag("DEBUG").debug("updateAfterSubsidiaryScreening - Watchlist target URL: {}", webTarget2.getUri());
        
        final Response response2 = webTarget2.request()
            .put(Entity.entity(screening.getWatchlistCompanyEntries(false), MediaType.APPLICATION_JSON));
        Logger.tag("DEBUG").debug("updateAfterSubsidiaryScreening - Watchlist response status: {}", response2.getStatus());
        
        final Family family2 = response2.getStatusInfo().getFamily();
        final String readEntity2 = response2.readEntity(String.class);
        
        if(family2 != Response.Status.Family.SUCCESSFUL) {
            Logger.tag("DEBUG").error("updateAfterSubsidiaryScreening - Watchlist save failed: {}", readEntity2);
            throw new RuntimeException("REST error: " + readEntity2);
        }
        
        Logger.tag("DEBUG").debug("updateAfterSubsidiaryScreening - Completed successfully");
    }

    public static boolean isBlocked(final Screening screening) {
        Logger.tag("DEBUG").debug("Entering isBlocked - Screening: {}", screening.getScreeningID());
        
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(TEMPLATE_FUNC_ISBLOCKED)
            .resolveTemplate("screeningId", screening.getScreeningID().toString());

        Logger.tag("DEBUG").debug("isBlocked - Target URL: {}", webTarget.getUri());
        
        final Response response = webTarget.request().get();
        Logger.tag("DEBUG").debug("isBlocked - Response status: {}", response.getStatus());
        
        if(response.getStatus() == 404) {
            Logger.tag("DEBUG").debug("isBlocked - Screening not found (404), assuming not blocked");
            return false;
        }
        
        boolean result = response.readEntity(Boolean.class);
        Logger.tag("DEBUG").debug("isBlocked - Result: {}", result);
        return result;
    }

    public static void deleteScreening(final String clientId, final String screeningId) {
        Logger.tag("DEBUG").debug("Entering deleteScreening - Client: {}, Screening: {}", clientId, screeningId);
        
        final WebTarget webTarget = RestUtil.getRestClient_ESRADB().getMethodTarget(TEMPLATE_ARCHIVED_SCREENING)
            .resolveTemplate("clientUuid", clientId)
            .resolveTemplate("screeningUuid", screeningId);

        Logger.tag("DEBUG").debug("deleteScreening - Target URL: {}", webTarget.getUri());
        
        final Response response = webTarget.request().delete();
        Logger.tag("DEBUG").debug("deleteScreening - Response status: {}", response.getStatus());
    }
}