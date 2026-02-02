# Architecture

Explanation of the data types in this system.

# What

This application serves as an API backend for the dashboard-platform project. It is a Frontend only React application,
where widgets can be added as plugins.

To make the backend keep up with this plugin architecture, the Frontend widgets can supply a recipe how the backend should act for the data of a widget.

# Anatomy of a widget

A widget has the following characteristics:

1. A widget type
2. A unique identifier
3. (optional) Child widgets
4. A configuration format
5. Configuration values
6. A description how data for the widget is managed (if any)

## Widget type and identifier

An unique identifier for the widget type.
The identifier is a unique database key for the widget

## Child widgets

A list of Id's of childs of this widget. For instance a grid widget can have children to fill the grid cells,
or a slideshow widget has child widgets that represent each slide.

## Configuration format

This format describes the data of a widget instance, including a recipe how the backend should act as data supplier for this widget.

It tells what configuration values a widget has, what the datatype of each value is, and if a value is a secret or not.

Example:

```
{
    widgetClass: "google-calendar-widget",
    configuration: {
        title: "Lunch & Learn binnenkort",
        secretIcalUrl: "https://.....",
        lookAhead: 60,
        lookBack: 0,
    },
    // Frontend settings
    configurationModel: [{
        id: "title",
        type: "string",
        scope: "frontend"
    }, {
        id: "secretIcalUrl",
        type: "string",
        scope: "backend",
        storage: "encrypted"
    }, {
        id: "lookAhead",
        type: "integer",
        scope: "frontend"
    }, {
        id: "lookAhead",
        type: "integer",
        scope: "backend"
    }],
    // Definition of custom endpoints on backend for widget
    dataEndpoints: [{
        path: "calendar",
        cache: 10*60*1000,
        steps: [{
            action: "tunnelRequest",
            config: {
                method: "GET",
                url: "%secretIcalUrl%",
            }
        }]
    }]
}

```
