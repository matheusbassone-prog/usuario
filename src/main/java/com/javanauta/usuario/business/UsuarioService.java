package com.javanauta.usuario.business;

import com.javanauta.usuario.business.converter.UsuarioConverter;
import com.javanauta.usuario.business.dto.UsuarioDTO;
import com.javanauta.usuario.infrastructure.entity.Usuario;
import com.javanauta.usuario.infrastructure.exceptions.ConflictException;
import com.javanauta.usuario.infrastructure.exceptions.ResourceNotFoundException;
import com.javanauta.usuario.infrastructure.repository.UsuarioRepository;
import com.javanauta.usuario.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioConverter usuarioConverter;
    private final JwtUtil jwtUtil;


    public UsuarioDTO salvaUsuario(UsuarioDTO usuarioDTO) {

        emailExiste(usuarioDTO.getEmail());

        Usuario usuario = usuarioConverter.paraUsuario(usuarioDTO);

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));

        return usuarioConverter.paraUsuarioDTO(
                usuarioRepository.save(usuario)
        );
    }

    public void emailExiste(String email){
        try{
            boolean existe = verificaEmailExistente(email);
            if(existe){
                throw new ConflictException("Email já cadastrado" + email);
            }
        }catch(ConflictException e){
            throw new ConflictException("Email já cadastrado", e.getCause());
        }
    }

    public boolean verificaEmailExistente(String email) {

        return usuarioRepository.existsByEmail(email);
    }

    public Usuario buscaUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("email não encontrado " + email));
    }

    public void deletaUsuarioPorEmail(String email) {

        usuarioRepository.deleteByEmail(email);
    }

    public UsuarioDTO atualizaDadosUsuario(String token, UsuarioDTO dto) {
        //Aqui buscamos o email do usuário através do token(tirar a obrigatoriedade do email)
        String email = jwtUtil.extrairEmailToken(token.substring(7));

        //criptografia de senha
        dto.setSenha(dto.getSenha() != null ? passwordEncoder.encode(dto.getSenha()) : null);

        //Busca os dados do usuário no banco de dados
        Usuario usuarioEntity = usuarioRepository.findByEmail(email).orElseThrow(() ->
                new ResourceNotFoundException("Email não localizado"));

        //Mesclou os dados que recebemos na requisição DTO com os dados do banco de dados
        Usuario usuario = usuarioConverter.updateUsuario(dto, usuarioEntity);

        return usuarioConverter.paraUsuarioDTO((usuarioRepository.save(usuario)));
    }


}
