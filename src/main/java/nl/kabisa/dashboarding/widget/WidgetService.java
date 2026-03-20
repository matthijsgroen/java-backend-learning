package nl.kabisa.dashboarding.widget;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.kabisa.dashboarding.user.UserNotFoundException;
import nl.kabisa.dashboarding.user.orm.User;
import nl.kabisa.dashboarding.user.orm.UserRepository;
import nl.kabisa.dashboarding.widget.configuration.ConfigurationExtractor;
import nl.kabisa.dashboarding.widget.configuration.ConfigurationValidator;
import nl.kabisa.dashboarding.widget.orm.Widget;
import nl.kabisa.dashboarding.widget.orm.WidgetRepository;

@Service
public class WidgetService {

    public record WidgetWithChildren(Widget widget, List<UUID> childIds) {}

    private final WidgetRepository widgetRepository;
    private final UserRepository userRepository;

    public WidgetService(WidgetRepository widgetRepository, UserRepository userRepository) {
        this.widgetRepository = widgetRepository;
        this.userRepository = userRepository;
    }

    private void assertIsOwner(Widget widget, UUID callerId) {
        if (!widget.getOwner().getId().equals(callerId)) {
            throw new WidgetAccessDeniedException();
        }
    }

    @Transactional
    public Widget createWidget(UUID callerId, CreateWidgetRequest request) {
        ConfigurationValidator.validate(request.configuration(), request.configurationModel());

        Map<String, Object> frontendConfiguration = ConfigurationExtractor.extractFrontend(
                request.configuration(), request.configurationModel());
        Map<String, Object> backendConfiguration = ConfigurationExtractor.extractBackend(
                request.configuration(), request.configurationModel());

        Widget parent = resolveParent(request.parentId());

        User owner = userRepository.findById(callerId)
                .orElseThrow(() -> new UserNotFoundException(callerId));

        Widget widget = new Widget();
        widget.setWidgetType(request.widgetType());
        widget.setVersion(request.version());
        widget.setFrontendConfiguration(frontendConfiguration);
        widget.setSecretsConfiguration(backendConfiguration);
        widget.setConfigurationModel(request.configurationModel());
        widget.setEndpoints(request.dataEndpoints());
        widget.setParent(parent);
        widget.setOwner(owner);

        return widgetRepository.save(widget);
    }

    /**
     * Fetches a widget and its direct child IDs in a single transaction for a consistent snapshot.
     */
    @Transactional(readOnly = true)
    public WidgetWithChildren getWidgetWithChildren(UUID callerId, UUID id) {
        Widget widget = widgetRepository.findById(id)
                .orElseThrow(() -> new WidgetNotFoundException(id));
        assertIsOwner(widget, callerId);
        List<UUID> childIds = widgetRepository.findChildIdsByParentId(id);
        return new WidgetWithChildren(widget, childIds);
    }

    @Transactional(readOnly = true)
    public Widget getWidget(UUID callerId, UUID id) {
        Widget widget = widgetRepository.findById(id)
                .orElseThrow(() -> new WidgetNotFoundException(id));
        assertIsOwner(widget, callerId);
        return widget;
    }

    @Transactional(readOnly = true)
    public List<Widget> getChildren(UUID callerId, UUID parentId) {
        Widget parent = widgetRepository.findById(parentId)
                .orElseThrow(() -> new WidgetNotFoundException(parentId));
        assertIsOwner(parent, callerId);
        return widgetRepository.findByParentId(parentId);
    }

    @Transactional
    public Widget updateWidget(UUID callerId, UUID widgetId, UpdateWidgetRequest request) {
        Widget widget = widgetRepository.findById(widgetId)
                .orElseThrow(() -> new WidgetNotFoundException(widgetId));
        assertIsOwner(widget, callerId);

        if (request.parentId() != null) {
            UUID parentUuid = parseUuid(request.parentId(), "parent widget id");
            validateNoCycle(widgetId, parentUuid);
            Widget newParent = widgetRepository.findById(parentUuid)
                    .orElseThrow(() -> new WidgetNotFoundException(parentUuid));
            widget.setParent(newParent);
        } else {
            widget.setParent(null);
        }

        return widgetRepository.save(widget);
    }

    @Transactional
    public int deleteWidgetWithDescendants(UUID callerId, UUID widgetId) {
        Widget widget = widgetRepository.findById(widgetId)
                .orElseThrow(() -> new WidgetNotFoundException(widgetId));
        assertIsOwner(widget, callerId);
        int result = widgetRepository.deleteWidgetTree(widgetId);
        if (result == 0) {
            // Race condition: widget was deleted between findById and deleteWidgetTree
            throw new WidgetNotFoundException(widgetId);
        }
        return result;
    }

    private void validateNoCycle(UUID widgetId, UUID proposedParentId) {
        if (widgetId.equals(proposedParentId)) {
            throw new WidgetCycleException(widgetId, proposedParentId);
        }
        // Native queries return UUIDs as strings in JDBC — compare as strings
        List<String> ancestorIds = widgetRepository.findAllAncestorIds(proposedParentId);
        if (ancestorIds.contains(widgetId.toString())) {
            throw new WidgetCycleException(widgetId, proposedParentId);
        }
    }

    private Widget resolveParent(String parentId) {
        if (parentId == null) {
            return null;
        }
        UUID parentUuid = parseUuid(parentId, "parent widget id");
        return widgetRepository.findById(parentUuid)
                .orElseThrow(() -> new WidgetNotFoundException(parentUuid));
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid " + fieldName, ex);
        }
    }
}
