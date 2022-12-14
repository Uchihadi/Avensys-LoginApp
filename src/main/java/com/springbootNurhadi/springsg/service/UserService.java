package com.springbootNurhadi.springsg.service;

import com.springbootNurhadi.springsg.Repo.UserRepo;
import com.springbootNurhadi.springsg.Request.UpdateReq;
import com.springbootNurhadi.springsg.Request.UserRequest;
import com.springbootNurhadi.springsg.Response.UserResponse;
import com.springbootNurhadi.springsg.model.UserModel;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    UserRepo userRepo;

    @Autowired
    Environment environment;

    public UserResponse register (UserRequest request) {
        UserResponse Response = new UserResponse();

        if (request.getEmail().equals("")) {
            Response.setMessage("Email Cannot be Empty");
            return Response;
        } else if (userRepo.getUserByEmail(request.getEmail()).isPresent()) {
            Response.setMessage("Email has already been registered!");
            return Response;
        }

        if (request.getPassword().equals("")) {
            Response.setMessage("Password Cannot be Empty");
            return Response;
        }

        if (request.getMobile().equals("")) {
            Response.setMessage("Mobile Number Cannot be Empty");
            return Response;
        } else {
            try {
                Integer.parseInt(request.getMobile());
            } catch (Exception e) {
                Response.setMessage("The Mobile Number is Invalid");
                return Response;
            }
        }

        if (request.getAddress().equals("")) {
            Response.setMessage("Address Cannot be Empty");
            return Response;
        }

        UserModel userModel = new UserModel(request.getMobile(), request.getEmail(),
                request.getPassword(), request.getAddress());
        userRepo.save(userModel);

        Response.setUserModel(userModel);
        Response.setMessage("User has been registered successfully!");
        return Response;
    }

    public UserModel getUserByEmail (String email) {
        if (!email.equals("")) {
            Optional <UserModel> Test = userRepo.getUserByEmail(email);

            if (Test.isPresent()){
                return Test.get();
            }
        }
        return null;
    }

    public UserResponse verifyEmailAndPassword (UserRequest userRequest) {
        UserResponse Response = new UserResponse();
        Optional<UserModel> Testing = userRepo.getUserByEmailAndPassword(userRequest.getEmail(), userRequest.getPassword());

        if (Testing.isPresent()) {
            Response.setUserModel(Testing.get());
            Response.setMessage("User Logged In Successfully");
        } else {
            Response.setMessage("Unable to Log In, please check your credentials");
        }
        return Response;
    }

    public boolean logout (UserRequest userRequest){
        UserModel userModel = getUserByEmail(userRequest.getEmail());
        if (userModel != null) {
            userRepo.updateTokenForUserId(null, userModel.getId());
            return true;
        }
        return false;
    }

    public UserResponse updateUser (UpdateReq updateRequest) {
        UserResponse Response = new UserResponse();

        if (updateRequest.getTarget().equals("")) {
            Response.setMessage("No User has been Selected");
        } else {
            Optional<UserModel> Test = userRepo.getUserByEmail(updateRequest.getTarget());

            if (!Test.isPresent()) {
                Response.setMessage("User is not Present");
            } else {
                UserModel userModel = Test.get();

                if (!updateRequest.getEmail().equals("")) {
                    userModel = new UserModel(updateRequest.getEmail(), userModel.getPassword(), userModel.getMobile(), userModel.getAddress());
                }

                if (!updateRequest.getPassword().equals("")) {
                    userModel.setPassword(updateRequest.getPassword());
                }

                if (!updateRequest.getMobile().equals("")) {
                    try {
                        Integer.parseInt(updateRequest.getMobile());
                    } catch (Exception e) {
                        Response.setMessage("Mobile Number is Invalid");
                        return Response;
                    }
                    userModel.setMobile(updateRequest.getMobile());
                }

                if (!updateRequest.getAddress().equals("")) {
                    userModel.setAddress(updateRequest.getAddress());
                }

                userRepo.delete(Test.get());
                userRepo.save(userModel);
                Response.setUserModel(userModel);
                Response.setMessage("User Updated Successfully!");
            }
        }
        return Response;
    }

    public UserResponse deleteUser (UpdateReq updateRequest) {
        UserResponse Response = new UserResponse();

        if (updateRequest.getEmail().equals("")) {
            Response.setMessage("No User has been selected");
        } else {
            Optional<UserModel> Test = userRepo.getUserByEmail(updateRequest.getEmail());

            if (!Test.isPresent()) {
                return new UserResponse("User Does Not Exist");
            } else {
                userRepo.delete(Test.get());
                userRepo.save(Test.get());
                Response.setMessage("User Deleted Successfully!");
            }
        }
        return Response;
    }

    public boolean validateToken (String token) throws Exception {
        Jwts.parser().setSigningKey(environment.getProperty("JWT_SECRET")).parseClaimsJws(token);
        return true;
    }

    private String generateToken(UserRequest req){
        UserModel user = getUserByEmail(req.getEmail());

        if (user != null) {
            Calendar expTime = Calendar.getInstance();
            expTime.add(Calendar.HOUR, 3);

            String token = Jwts.builder()
                    .claim("email", user.getEmail())
                    .setId("" + user.getId())
                    .setIssuedAt(new Date())
                    .setExpiration(expTime.getTime())
                    .signWith(SignatureAlgorithm.HS512, environment.getProperty("JWT_SECRET"))
                    .compact();

            return token;
        }

        return null;
    }

}


