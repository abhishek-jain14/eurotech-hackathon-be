# Spring Boot E-Commerce CRUD API

A simple and clean RESTful API for managing an e-commerce product catalog. 

This project is built using Java and Spring Boot, and uses H2 in-memory database for easy testing and development.

---

## Features
Full CRUD operations for Products

- Create new products
- Read all products or a product by ID
- Update existing products
- Delete products by ID

Product attributes include:

- Name, description, brand, price, category, release date, availability, and quantity

Uses Spring Data JPA with H2 in-memory database (default) for persistence

Easy to extend for more e-commerce functionalities

Clean REST endpoints under /api

---

## Technologies Used
- Java
- Spring Boot
- Spring Data JPA
- H2 Database (in-memory)
- Lombok for boilerplate code reduction
- Maven as the build tool

---

## API Endpoints
| Method | Endpoint            | Description                | Request Body        |
| ------ | ------------------- | -------------------------- | ------------------- |
| GET    | `/api/products`     | Get list of all products   | None                |
| GET    | `/api/product/{id}` | Get product by ID          | None                |
| POST   | `/api/products`     | Add a new product          | JSON Product object |
| PUT    | `/api/product`      | Update an existing product | JSON Product object |
| DELETE | `/api/product/{id}` | Delete a product by ID     | None                |

## Getting Started

### Prerequisites
- Java
- Maven 
- Postman for API testing

### How to Run
1. Clone the repository:

```
git clone https://github.com/premswaroopmusti/springboot_E-Commerce_crud_api.git
cd springboot_E-Commerce_crud_api
```

2. Build and run the application using Maven:

```
./mvnw spring-boot:run
```

3. The API will be available at:

```
http://localhost:8080/api/
```

4. Example: Get all products

```
GET http://localhost:8080/api/products
```

## Database
- Uses H2 in-memory database by default, no setup needed.

- Data is initialized from data.sql on startup, which preloads sample products.

- You can configure an external database by updating
```
src/main/resources/application.properties.
```

## Sample Product JSON Object
```
json
{
  "id": 1,
  "name": "Tata Nexon",
  "desc": "A compact SUV with excellent safety features and performance.",
  "brand": "Tata Motors",
  "price": 750000.00,
  "category": "Cars",
  "releaseDate": "15-01-2024",
  "available": true,
  "quantity": 50
}
```

## Project Structure
- model: Product entity with JPA annotations
- repository: ProductRepo interface extending JpaRepository for database operations
- service: ProductService with business logic for CRUD
- controller: ProductController exposing REST endpoints

## Notes
- Dates use the format "dd-MM-yyyy" as per @JsonFormat in the model.
- Error handling returns HTTP status codes and messages where applicable.
- The project uses Lombok annotations for getters/setters, constructors, and reduces boilerplate.

## Contributing
Feel free to fork the repo and send pull requests! Suggestions and improvements are welcome.

