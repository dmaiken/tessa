# tessA

tessA is a high-performance, non-blocking REST API for managing assets such as images and media files.
Built with Kotlin, it provides a flexible, path-driven design that allows clients to define their own hierarchical
structure for asset storage and retrieval.

## Features

🚀 100% non-blocking I/O using Kotlin coroutines and Netty

🌐 REST API (with support for future protocols like gRPC)

🧩 Composable asset paths – you define the structure

🎯 Efficient streaming of large asset content

🗂️ Support for metadata, direct content access, and redirects

🧼 Minimal configuration, strong conventions

## Path-based Design

tessA's API design is inspired from the idea that the storing of assets is inherently hierarchical, much like a
directory structure. This pairs nicely with a REST API where resources are accessed by their path and "own" resources
defined underneath them. tessA doesn't try to enforce any particular structure for assets, but it does provide a
simple way to define your own.

The way that tessA stores and references the asset tree means there is no performance penalty for any path structure you
choose.

Assets are stored according to their URL path when persisting them. So, if you wished to store a user's profile photo,
you could do
something like this:

```
POST:/assets/users/{userId}/profilePicture
```

To retrieve this image:

```
GET:/assets/users/{userId}/profilePicture
```

Assets are assigned an increasing `entryId` within their path. In the example above, calling:

```
GET:/assets/users/{userId}/profilePicture?entryId=0
```

will return the same image

## Multiple Assets in Path

What if the user has an album? Easy. tessA provides a solution for that. Let's say your user went on a ski trip:

```
POST:/assets/users/{userId}/ski-trip
POST:/assets/users/{userId}/ski-trip
```

When assets are persisted to a specific path, they are "pushed" into the path much like a stack. Calling:

```
GET:/assets/users/{userId}/ski-trip
```

will return the latest image persisted. Other images in the path can be accessed via their `entryId`.

## Userless Assets

What if I don't have users? No problem! tessA is incredibly generic and adaptable to your specific use case. Let's take
a
common example, a simple AirBnB-type listing:

```
// Post images to your listing
POST:/assets/{listingId}

// I want to organize them by room/area
POST:/assets/{listingId}/kitchen // creates entryId: 0
POST:/assets/{listingId}/kitchen // creates entryId: 1
```

# Just Desserts

If Asset storage was all tessA did, you would use AWS S3 or even a SAN. tessA is more powerful though. In addition to
asset storage,
tessA provides:

- asset processing (image resizing, filetype conversion)
- returning asset content directly from tessA or returning a redirect to the backing object store
- an image transformation API built on Libvips, the standard in image processing performance (and no JNI is used) (*
  *coming soon!**)
- asset metadata
- on-the-fly variant generation or variant caching including precomputation of common image variants at upload time (*
  *all coming soon!**)
- image compression at storage and/or over the network (**coming soon!**)
- Lots of asset types (currently only image types are supported but **more to come!**)
- Template generation (**coming soon!**)
- Default assets based on path patterns (**coming soon!**)
- OAuth based on path patterns (**coming soon!**)

## Building & Running

To build or run the project, use one of the following tasks:

| Task                          | Description                                                          |
|-------------------------------|----------------------------------------------------------------------|
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `buildImage`                  | Build the docker asset to use with the fat JAR                       |
| `publishImageToLocalRegistry` | Publish the docker asset locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run using the local docker asset                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

